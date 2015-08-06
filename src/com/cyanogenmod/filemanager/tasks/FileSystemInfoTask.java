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

package com.cyanogenmod.filemanager.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.ui.fragments.NavigationFragment;
import com.cyanogenmod.filemanager.util.MountPointHelper;

import java.lang.ref.WeakReference;

/**
 * A class for recovery information about filesystem status (mount point, disk usage, ...).
 */
public class FileSystemInfoTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = FileSystemInfoTask.class.getSimpleName();

    private WeakReference<NavigationFragment> mNavigationFragmentWeakReference;

    final int mFreeDiskSpaceWarningLevel;
    private boolean mRunning;
    final boolean mIsDialog;

    private MountPoint mMountPoint;
    private DiskUsage mDiskUsage;

    /**
     * Constructor of <code>FileSystemInfoTask</code>.
     *
     * @param freeDiskSpaceWarningLevel The free disk space warning level
     */
    public FileSystemInfoTask(NavigationFragment navigationFragment,
            int freeDiskSpaceWarningLevel) {
        this(navigationFragment, freeDiskSpaceWarningLevel, false);
    }

    /**
     * Constructor of <code>FileSystemInfoTask</code>.
     *
     * @param freeDiskSpaceWarningLevel The free disk space warning level
     * @param isDialog Whether or not to use dialog theme resources
     */
    public FileSystemInfoTask(NavigationFragment navigationFragment,
            int freeDiskSpaceWarningLevel, boolean isDialog) {
        super();
        mNavigationFragmentWeakReference = new WeakReference<>(navigationFragment);
        this.mFreeDiskSpaceWarningLevel = freeDiskSpaceWarningLevel;
        this.mRunning = false;
        this.mIsDialog = isDialog;
    }

    /**
     * Method that returns if there is a task running.
     *
     * @return boolean If there is a task running
     */
    public boolean isRunning() {
        return this.mRunning;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void doInBackground(String... params) {
        //Running
        this.mRunning = true;

        //Extract the directory from arguments
        String dir = params[0];

        //Extract filesystem mount point from directory
        if (isCancelled()) {
            return null;
        }

        mMountPoint = MountPointHelper.getMountPointFromDirectory(dir);
        if (mMountPoint == null) {
            //There is no information about this filesystem
            if (isCancelled()) {
                return null;
            }
        } else {
            //Load information about disk usage
            if (isCancelled()) {
                return null;
            }
            mDiskUsage = null;
            mDiskUsage = MountPointHelper.getMountPointDiskUsage(mMountPoint);

            int usage = 0;
            if (mDiskUsage != null && mDiskUsage.getTotal() != 0) {
                usage = (int) (mDiskUsage.getUsed() * 100 / mDiskUsage.getTotal());
            } else {
                usage = mDiskUsage == null ? 0 : 100;
            }

            // Advise about diskusage (>=mFreeDiskSpaceWarningLevel) with other color
            if (usage >= mFreeDiskSpaceWarningLevel) {
                Log.i(TAG, "It's all good");
            } else {
                Log.i(TAG, "Over warning");
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(Void aVoid) {
        this.mRunning = false;
        NavigationFragment fragment = mNavigationFragmentWeakReference.get();
        if (fragment != null) {
            fragment.setMountPoint(mMountPoint);
            fragment.setDiskUsage(mDiskUsage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled(Void aVoid) {
        this.mRunning = false;
        super.onCancelled(aVoid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled() {
        this.mRunning = false;
        super.onCancelled();
    }

}
