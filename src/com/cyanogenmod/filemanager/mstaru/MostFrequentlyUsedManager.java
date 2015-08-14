/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.mstaru;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.commands.shell.InvalidCommandDefinitionException;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.OperationTimeoutException;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.mstaru.MostFrequentlyUsedContract.Item;
import com.cyanogenmod.filemanager.util.CommandHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is meant to only be used with the Application Context.  If you break it, you bought it.
 */
public class MostFrequentlyUsedManager implements IMostStarUsedFilesManager {
    private static final String TAG = MostFrequentlyUsedManager.class.getSimpleName();

    private static class UIHandler extends Handler {
        public static final int MSG_ITEMS = 0;

        private WeakReference<MostFrequentlyUsedManager> mMgr;

        public UIHandler(MostFrequentlyUsedManager mgr) {
            super(Looper.getMainLooper());
            mMgr = new WeakReference<>(mgr);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_ITEMS:
                    Log.d(TAG, "Notifying of changes");
                    MostFrequentlyUsedManager mgr = mMgr.get();
                    if (mgr != null) {
                        mgr.notifyChange((List<FileSystemObject>) msg.obj);
                    }
                    break;
            }
        }
    }

    private static final String[] PROJECTION = {
            Item.KEY
    };
    private static final int PROJECTION_KEY = 0;

    private ContentResolver mResolver;

    private ContentObserver mObserver;
    private HandlerThread mThread;

    private Handler mUIHandler = new UIHandler(this);

    private Set<IFileObserver> mObservers;

    private List<FileSystemObject> mFiles;

    /* package */ MostFrequentlyUsedManager(@NonNull Context context) {
        context = context.getApplicationContext();
        mResolver = context.getContentResolver();
        mObservers = new HashSet<>();
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    private static FileSystemObject parseKey(@NonNull String key) {
        try {
            return CommandHelper.getFileInfo(
                    FileManagerApplication.getInstance(), key, false, null);
        } catch (InsufficientPermissionsException
                | InvalidCommandDefinitionException
                | ConsoleAllocException
                | ExecutionException
                | OperationTimeoutException
                | NoSuchFileOrDirectory
                | CommandNotFoundException
                | IOException e) {
            return null;
        }
    }

    private static String keyFor(@NonNull FileSystemObject fso) {
        Log.d(TAG, "Generating key for " + fso.getFullPath());
        return fso.getFullPath();
    }

    @Override
    public boolean notifyAccessed(@NonNull FileSystemObject fso) throws IllegalStateException {
        if (isMainThread()) {
            throw new IllegalStateException("Must not be invoked from the main thread.");
        }

        ContentValues values = new ContentValues();
        values.put(MostFrequentlyUsedContract.Item.KEY, keyFor(fso));

        mResolver.insert(Item.CONTENT_URI, values);
        return true;
    }

    @Override
    public void notifyAccessedAsync(@NonNull final FileSystemObject fso) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void ... params) {
                notifyAccessed(fso);
                return null;
            }
        }.execute();
    }

    @Override
    public boolean notifyMoved(@NonNull FileSystemObject from,
                               @NonNull FileSystemObject to) {
        if (isMainThread()) {
            throw new IllegalStateException("Must not be invoked from the main thread.");
        }

        ContentValues values = new ContentValues();
        values.put(MostFrequentlyUsedContract.Item.KEY, keyFor(to));

        mResolver.update(Item.CONTENT_URI,
                values,
                MostFrequentlyUsedContract.Item.KEY + "=?",
                new String[]{
                        keyFor(from),
                });
        return true;
    }

    @Override
    public void notifyMovedAsync(@NonNull final FileSystemObject from,
                                 @NonNull final FileSystemObject to) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void ... params) {
                notifyMoved(from, to);
                return null;
            }
        }.execute();
    }

    @Override
    public boolean notifyDeleted(@NonNull FileSystemObject fso) {
        if (isMainThread()) {
            throw new IllegalStateException("Must not be invoked from the main thread.");
        }

        mResolver.delete(MostFrequentlyUsedContract.Item.CONTENT_URI,
                MostFrequentlyUsedContract.Item.KEY + "=?",
                new String[]{
                        keyFor(fso)
                });
        return true;
    }

    @Override
    public void notifyDeletedAsync(@NonNull final FileSystemObject fso) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void ... params) {
                notifyDeleted(fso);
                return null;
            }
        }.execute();
    }

    @Override
    public List<FileSystemObject> getFiles() {
        if (isMainThread()) {
            throw new IllegalStateException("Must not be invoked from the main thread.");
        }

        ArrayList<FileSystemObject> files;
        Cursor c = null;
        try {
            files = new ArrayList<FileSystemObject>();
            c = mResolver.query(Item.CONTENT_URI, PROJECTION, null, null, null);
            while (c.moveToNext()) {
                FileSystemObject o = parseKey(c.getString(PROJECTION_KEY));
                if (o != null) {
                    files.add(o);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return files;
    }

    @Override
    public void registerObserver(@NonNull IFileObserver observer) {
        if (!isMainThread()) {
            throw new IllegalStateException("Must be invoked on the main thread.");
        }
        mObservers.add(observer);

        if (mObserver == null) {
            mThread = new HandlerThread("FrequentObserver");
            mThread.start();

            mObserver = new ContentObserver(new Handler(mThread.getLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    List<FileSystemObject> files = getFiles();
                    Log.d(TAG, "Got " + files.size() + " items");
                    // post back to the main thread
                    mUIHandler.obtainMessage(UIHandler.MSG_ITEMS, files).sendToTarget();
                }
            };
            mResolver.registerContentObserver(Item.CONTENT_URI, true, mObserver);
            // kick it off, so we get the first item
            mObserver.dispatchChange(false, Item.CONTENT_URI);
        } else {
            if (mFiles == null) {
                notifyChange(mFiles);
            } else {
                // either we haven't received a response yet, or it was null, either way, we don't
                // need to dispatch it again
            }
        }
    }

    @Override
    public void unregisterObserver(@NonNull IFileObserver observer) {
        if (!isMainThread()) {
            throw new IllegalStateException("Must be invoked on the main thread.");
        }

        mObservers.remove(observer);
        if (mObservers.isEmpty()) {
            mResolver.unregisterContentObserver(mObserver);
            mObserver = null;
            mThread.getLooper().quit();
        }
    }

    /* package */ void notifyChange(List<FileSystemObject> files) {
        mFiles = files;
        for (IFileObserver o : mObservers) {
            o.onFilesChanged(files);
        }
    }
}
