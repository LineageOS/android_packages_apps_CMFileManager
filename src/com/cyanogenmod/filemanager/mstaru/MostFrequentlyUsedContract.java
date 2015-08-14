package com.cyanogenmod.filemanager.mstaru;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by herriojr on 8/10/15.
 */
/* package */ interface MostFrequentlyUsedContract {
    String AUTHORITY = "com.cyanogenmod.filemanager.mfu";

    class Item implements BaseColumns {
        /* package */ static final String TABLE_NAME = "items";

        public static final String FOLDER = TABLE_NAME;

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + AUTHORITY + "." + FOLDER;

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + FOLDER);

        /* package */ static final String _LAST_DEGRADE_TIMESTAMP = "_last_degrade";
        public static final String KEY = "key";
        public static final String COUNT = "count";
    }
}
