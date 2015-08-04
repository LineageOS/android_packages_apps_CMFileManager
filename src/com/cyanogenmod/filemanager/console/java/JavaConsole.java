/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.console.java;

import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.commands.ExecutableFactory;
import com.cyanogenmod.filemanager.commands.java.JavaExecutableFactory;
import com.cyanogenmod.filemanager.commands.java.Program;
import com.cyanogenmod.filemanager.console.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of a {@link VirtualConsole} based on a java implementation.<br/>
 * <br/>
 * This console is a non-privileged console an many of the functionality is not implemented
 * because can't be obtain from java api.
 */
public final class JavaConsole extends VirtualConsole {

    private static final String TAG = "JavaConsole"; //$NON-NLS-1$

    /**
     * INotify works in this case, so we just use INotify to handle it
     */
    private static class NativeFileObserver extends FileObserver {
        private Set<ConsoleFileObserver> mObservers;

        public NativeFileObserver(String path) {
            super(path);
            mObservers = new HashSet<ConsoleFileObserver>();
        }

        public synchronized void registerObserver(ConsoleFileObserver observer) {
            mObservers.add(observer);
        }

        public synchronized void unregisterObserver(ConsoleFileObserver observer) {
            mObservers.remove(observer);
        }

        @Override
        public synchronized void onEvent(int event, String path) {
            for (ConsoleFileObserver observer : mObservers) {
                observer.onEvent(event, path);
            }
        }

        public synchronized int getCount() {
            return mObservers.size();
        }
    }

    private final int mBufferSize;
    private Program mActiveProgram;

    private HashMap<String, NativeFileObserver> mObservers;

    /**
     * Constructor of <code>JavaConsole</code>
     *
     * @param ctx The current context
     * @param bufferSize The buffer size
     */
    public JavaConsole(Context ctx, int bufferSize) {
        super(ctx);
        this.mBufferSize = bufferSize;
        mObservers = new HashMap<String, NativeFileObserver>();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Java";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutableFactory getExecutableFactory() {
        return new JavaExecutableFactory(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void execute(Executable executable, Context ctx)
            throws ConsoleAllocException, InsufficientPermissionsException, NoSuchFileOrDirectory,
                OperationTimeoutException, ExecutionException, CommandNotFoundException,
                   CancelledOperationException, ReadOnlyFilesystemException {
        // Check that the program is a java program
        try {
            Program p = (Program)executable;
            p.isTrace();
        } catch (Throwable e) {
            Log.e(TAG, String.format("Failed to resolve program: %s", //$NON-NLS-1$
                    executable.getClass().toString()), e);
            throw new CommandNotFoundException("executable is not a program", e); //$NON-NLS-1$
        }

        //Auditing program execution
        if (isTrace()) {
            Log.v(TAG, String.format("Executing program: %s", //$NON-NLS-1$
                    executable.getClass().toString()));
        }

        // Execute the program
        final Program program = (Program)executable;
        mActiveProgram = program;
        program.setTrace(isTrace());
        program.setBufferSize(this.mBufferSize);
        if (program.isAsynchronous()) {
            // Execute in a thread
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        program.execute();
                    } catch (Exception e) {
                        // Program must use onException to communicate exceptions
                        Log.v(TAG,
                                String.format("Async execute failed program: %s", //$NON-NLS-1$
                                program.getClass().toString()));
                    }
                }
            };
            t.start();

        } else {
            // Synchronous execution
            program.execute();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCancel() {
        mActiveProgram.requestCancel();
        return true;
    }

    @Override
    public synchronized void registerFileObserver(String path, ConsoleFileObserver observer) {
        NativeFileObserver no = mObservers.get(path);
        if (no == null) {
            no = new NativeFileObserver(path);
            mObservers.put(path, no);
            no.startWatching();
        }
        no.registerObserver(observer);
    }

    @Override
    public synchronized void unregisterFileObserver(String path, ConsoleFileObserver observer) {
        NativeFileObserver no = mObservers.get(path);
        if (no == null) {
            return;
        }

        no.unregisterObserver(observer);
        if (no.getCount() == 0) {
            no.stopWatching();
            mObservers.remove(path);
        }
    }
}