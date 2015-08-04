package com.cyanogenmod.filemanager.commands;

import android.util.Log;
import com.cyanogenmod.filemanager.console.ConsoleFileObserver;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by herriojr on 8/3/15.
 */
public class NotifyObserversUtil {
    private static final String TAG = NotifyObserversUtil.class.getSimpleName();


    public static void notifyCreated(String path, HashMap<String, Set<ConsoleFileObserver>> observers) {
        Log.d(TAG, "Notify created " + path);

        String parent = FileHelper.getParentDir(path);
        String current = FileHelper.basename(path);

        // First notify any listeners on the parent
        Set<ConsoleFileObserver> set = observers.get(parent);
        if (set != null) {
            for (ConsoleFileObserver o : set) {
                o.onEvent(ConsoleFileObserver.CREATE, current);
            }
        }
    }

    public static void notifyDeleted(String path, HashMap<String, Set<ConsoleFileObserver>> observers) {
        Log.d(TAG, "Notify Deleted " + path);
        String parent = FileHelper.getParentDir(path);
        String current = FileHelper.basename(path);

        // First notify any listeners on the parent
        Set<ConsoleFileObserver> set = observers.get(parent);
        if (set != null) {
            for (ConsoleFileObserver o : set) {
                o.onEvent(ConsoleFileObserver.DELETE, current);
            }
        }

        // Then notify anything listening on this file
        set = observers.get(path);
        if (set != null) {
            for (ConsoleFileObserver o : set) {
                o.onEvent(ConsoleFileObserver.DELETE_SELF, "");
            }
        }
    }

    /**
     * Notifies that the src file or folder is moved to an *existing* dst directory
     *
     * @param src
     * @param dst
     * @param observers
     */
    public static void notifyMoved(String src, String dst, HashMap<String, Set<ConsoleFileObserver>> observers) {
        Log.d(TAG, "Notify Moved from " + src + " to " + dst);
        String parentFrom = FileHelper.getParentDir(src);
        String from = FileHelper.basename(src);

        // First notify any listeners on the parent
        Set<ConsoleFileObserver> set = observers.get(parentFrom);
        if (set != null) {
            for (ConsoleFileObserver o : set) {
                o.onEvent(ConsoleFileObserver.MOVED_FROM, from);
                o.onEvent(ConsoleFileObserver.MOVED_TO, dst);
            }
        }

        // Then notify any listeners on the file
        set = observers.get(from);
        if (set != null) {
            for (ConsoleFileObserver o : set) {
                o.onEvent(ConsoleFileObserver.MOVE_SELF, null);
            }
        }
    }

    public static void notifyAttrib(String path, HashMap<String, Set<ConsoleFileObserver>> observers) {
        Log.d(TAG, "Notify Property Change on " + path);
        String parent = FileHelper.getParentDir(path);
        String current = FileHelper.basename(path);

        // First notify any listeners on the parent
        Set<ConsoleFileObserver> set = observers.get(parent);
        if (set != null) {
            for (ConsoleFileObserver o : set) {
                o.onEvent(ConsoleFileObserver.ATTRIB, current);
            }
        }

        // Then notify any listeners on the file
        set = observers.get(path);
        if (set != null) {
            for (ConsoleFileObserver o : set) {
                o.onEvent(ConsoleFileObserver.ATTRIB, null);
            }
        }
    }
}
