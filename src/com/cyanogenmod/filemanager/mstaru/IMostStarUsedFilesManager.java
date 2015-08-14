package com.cyanogenmod.filemanager.mstaru;

import android.content.Context;
import com.cyanogenmod.filemanager.model.FileSystemObject;

import java.util.List;

/**
 * Created by herriojr on 8/10/15.
 */
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
