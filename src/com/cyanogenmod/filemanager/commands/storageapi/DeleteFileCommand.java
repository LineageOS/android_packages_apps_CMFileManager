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

package com.cyanogenmod.filemanager.commands.storageapi;

import android.util.Log;
import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogenmod.filemanager.commands.DeleteFileExecutable;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.MountPoint;


/**
 * A class for delete a file.
 */
public class DeleteFileCommand extends Program implements DeleteFileExecutable {

    private static final String TAG = "DeleteFileCommand"; //$NON-NLS-1$

    private final String mPath;
    private final StorageApiConsole mConsole;
    private boolean mResult = false;

    /**
     * Constructor of <code>DeleteFileCommand</code>.
     *
     * @param path The name of the new file
     */
    public DeleteFileCommand(StorageApiConsole console, String path) {
        super();
        this.mConsole = console;
        this.mPath = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResult() {
        return mResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
            throws InsufficientPermissionsException, NoSuchFileOrDirectory, ExecutionException {
        if (isTrace()) {
            Log.v(TAG,
                    String.format("Deleting file: %s", this.mPath)); //$NON-NLS-1$
        }

        String path = StorageApiConsole.getProviderPathFromFullPath(mPath);
        PendingResult<StorageApi.StatusResult> pendingResult = mConsole.getStorageApi().delete(
                mConsole.getStorageProviderInfo(), path);
        StorageApi.StatusResult statusResult = pendingResult.await();

        mResult = statusResult.getCommonStatus().isSuccess();

        if (isTrace()) {
            Log.v(TAG, "Result: " + mResult); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPoint getSrcWritableMountPoint() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPoint getDstWritableMountPoint() {
        return null;
    }
}
