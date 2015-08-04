package com.cyanogenmod.filemanager.console;

import android.os.FileObserver;

/**
 * Created by herriojr on 8/3/15.
 */
public interface ConsoleFileObserver {
    /** Event type: Data was read from a file */
    public static final int ACCESS = FileObserver.ACCESS;
    /** Event type: Data was written to a file */
    public static final int MODIFY = FileObserver.MODIFY;
    /** Event type: Metadata (permissions, owner, timestamp) was changed explicitly */
    public static final int ATTRIB = FileObserver.ATTRIB;
    /** Event type: Someone had a file or directory open for writing, and closed it */
    public static final int CLOSE_WRITE = FileObserver.CLOSE_WRITE;
    /** Event type: Someone had a file or directory open read-only, and closed it */
    public static final int CLOSE_NOWRITE = FileObserver.CLOSE_NOWRITE;
    /** Event type: A file or directory was opened */
    public static final int OPEN = FileObserver.OPEN;
    /** Event type: A file or subdirectory was moved from the monitored directory */
    public static final int MOVED_FROM = FileObserver.MOVED_FROM;
    /** Event type: A file or subdirectory was moved to the monitored directory */
    public static final int MOVED_TO = FileObserver.MOVED_TO;
    /** Event type: A new file or subdirectory was created under the monitored directory */
    public static final int CREATE = FileObserver.CREATE;
    /** Event type: A file was deleted from the monitored directory */
    public static final int DELETE = FileObserver.DELETE;
    /** Event type: The monitored file or directory was deleted; monitoring effectively stops */
    public static final int DELETE_SELF = FileObserver.DELETE_SELF;
    /** Event type: The monitored file or directory was moved; monitoring continues */
    public static final int MOVE_SELF = FileObserver.MOVE_SELF;

    /** Event mask: All valid event types, combined */
    public static final int ALL_EVENTS = FileObserver.ALL_EVENTS;
    
    void onEvent(int event, String path);
}
