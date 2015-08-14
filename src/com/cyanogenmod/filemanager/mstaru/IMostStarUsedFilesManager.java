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

import android.content.Context;
import com.cyanogenmod.filemanager.model.FileSystemObject;

import java.util.List;

public interface IMostStarUsedFilesManager {
    class Factory {
        public static IMostStarUsedFilesManager newInstance(Context context) {
            return new MostFrequentlyUsedManager(context);
        }

        private Factory() {}
    }

    interface IFileObserver {
        void onFilesChanged(List<FileSystemObject> files);
    }

    /**
     * Registers that a file has been accessed.
     *
     * @param fso The file system object
     *
     * @return Whether the operation succeeded or not
     */
    boolean notifyAccessed(FileSystemObject fso);

    void notifyAccessedAsync(FileSystemObject fso);

    /**
     * Registers that a file system object has been moved.
     *
     * @param from The original location of the object
     * @param to The new location of the object
     *
     * @return Whether the operation succeeded or not
     */
    boolean notifyMoved(FileSystemObject from, FileSystemObject to);

    void notifyMovedAsync(FileSystemObject from, FileSystemObject to);

    /**
     * Registers that a file system object has been deleted.
     *
     * @param fso The file system object
     *
     * @return Whether the operation succeeded or not
     */
    boolean notifyDeleted(FileSystemObject fso);

    void notifyDeletedAsync(FileSystemObject fso);

    /**
     * Gets the list of important files we may want to show. If you need updates as things change
     * consider using the register/unregister
     *
     * @return The list of files or null if there was a problem.
     */
    List<FileSystemObject> getFiles();

    void registerObserver(IFileObserver observer);

    void unregisterObserver(IFileObserver observer);
}
