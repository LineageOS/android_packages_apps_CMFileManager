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

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.app.Activity;

import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.fragments.NavigationFragment;
import com.cyanogenmod.filemanager.util.MountPointHelper;

/**
 * A class for recovery information about filesystem status (mount point, disk usage, ...).
 */
public class FileSystemInfoTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = FileSystemInfoTask.class.getSimpleName();

    private NavigationFragment mNavigationFragment;

    final int mFreeDiskSpaceWarningLevel;
    private boolean mRunning;

    final boolean mIsDialog;

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
        this.mNavigationFragment = navigationFragment;
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

        MountPoint mountPoint = MountPointHelper.getMountPointFromDirectory(dir);
        mNavigationFragment.setMountPoint(mountPoint);
        if (mountPoint == null) {
            //There is no information about this filesystem
            if (isCancelled()) {
                return null;
            }
        } else {
            //Load information about disk usage
            if (isCancelled()) {
                return null;
            }
            DiskUsage diskUsage = null;
            diskUsage = MountPointHelper.getMountPointDiskUsage(mountPoint);

            mNavigationFragment.setDiskUsage(diskUsage);
            int usage = 0;
            if (diskUsage != null && diskUsage.getTotal() != 0) {
                usage = (int) (diskUsage.getUsed() * 100 / diskUsage.getTotal());
            } else {
                usage = diskUsage == null ? 0 : 100;
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
