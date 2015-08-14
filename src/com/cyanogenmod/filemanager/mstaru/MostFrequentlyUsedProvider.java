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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;

import android.util.Log;
import com.cyanogenmod.filemanager.mstaru.MostFrequentlyUsedContract.Item;

import java.util.HashMap;

/**
 * This provider provides access to help in keeping track and querying of the most frequently
 * accessed items.
 */
public class MostFrequentlyUsedProvider extends ContentProvider {
    private static final String TAG = MostFrequentlyUsedProvider.class.getSimpleName();

    private static final boolean DEBUG = true;

    private static class MFUOpenHelper extends SQLiteOpenHelper {

        private static final String NAME = "mfu";
        private static final int VERSION = 1;

        /**
         * {@inheritDoc}
         */
        public MFUOpenHelper(Context context) {
            super(context, NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + MostFrequentlyUsedContract.Item.TABLE_NAME + "("
                            + MostFrequentlyUsedContract.Item._ID
                            + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + Item.KEY + " TEXT,"
                            + Item.COUNT + " INTEGER NOT NULL DEFAULT 1,"
                            + Item._LAST_DEGRADE_TIMESTAMP
                            + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "UNIQUE(" + MostFrequentlyUsedContract.Item.KEY + ')'
                            + ");"
             );

            db.execSQL("CREATE INDEX " + Item.TABLE_NAME + "_count_idx"
                    + " ON " + Item.TABLE_NAME + "(" + Item.COUNT + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // No upgrades yet
        }
    }

    private static final String STMT_UPDATE_COUNT = "UPDATE " + Item.TABLE_NAME
            + " SET " + Item.COUNT + "=" + Item.COUNT + "+1, "
                      + Item._LAST_DEGRADE_TIMESTAMP + "=CURRENT_TIMESTAMP"
            + " WHERE " + Item.KEY + "=?;";

    private static final String STMT_DEGRADE_COUNT = "UPDATE " + Item.TABLE_NAME
            + " SET " + Item.COUNT +"=" + Item.COUNT + "-CAST("
                    + "JULIANDAY(CURRENT_TIMESTAMP)"
                    + "-JULIANDAY(" + Item._LAST_DEGRADE_TIMESTAMP + ")"
                + " AS INT),"
                + Item._LAST_DEGRADE_TIMESTAMP + "=CURRENT_TIMESTAMP"
            + " WHERE "
                + "JULIANDAY() - JULIANDAY(" + Item._LAST_DEGRADE_TIMESTAMP + ") > 1;";


    private static final int MAX_RESULTS = 10;

    private static UriMatcher sUriMatcher;

    private static final int MATCH_ITEMS = 0;

    private static HashMap<String, String> sFilesProjectionMap;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(MostFrequentlyUsedContract.AUTHORITY, "/" + Item.FOLDER, MATCH_ITEMS);

        sFilesProjectionMap = new HashMap<String, String>();
        sFilesProjectionMap.put(Item._ID, Item._ID);
        sFilesProjectionMap.put(MostFrequentlyUsedContract.Item.KEY,
                MostFrequentlyUsedContract.Item.KEY);
        sFilesProjectionMap.put(Item.COUNT, Item.COUNT);
    }

    private MFUOpenHelper mOpenHelper;

    private ContentResolver mResolver;

    @Override
    public boolean onCreate() {
        mOpenHelper = new MFUOpenHelper(getContext());
        mResolver = getContext().getContentResolver();

        return true;
    }

    /**
     * This degrades all columns by the number of days since the last degrade for each column.
     *
     * Eventually, everything will end up in the negatives, however, given our logic, we always keep
     * at least {@link #MAX_RESULTS} items if that number exists.
     *
     * @param db
     */
    private int degrade(SQLiteDatabase db) {
        try {
            SQLiteStatement stmt = db.compileStatement(STMT_DEGRADE_COUNT);
            int cnt = stmt.executeUpdateDelete();

            if (DEBUG) Log.d(TAG, "Degraded " + cnt + " items");

            return cnt;
        } catch(SQLiteException e) {
            Log.e(TAG, "Failed to degrade ", e);
            // Do nothing, we'll try again some other time
        }
        return 0;
    }

    private int prune(SQLiteDatabase db) {
        Cursor c;
        try {
            c = db.query(Item.TABLE_NAME, new String[]{Item._ID}, null, null, null, null,
                    Item.COUNT + " DESC", "" + MAX_RESULTS);
            StringBuilder b = new StringBuilder();

            String[] selectionArgs = new String[c.getCount()];
            b.append(Item.COUNT)
                    .append(" < 0");
            int i = 0;

            while (c.moveToNext()) {
                b.append(" AND ")
                        .append(Item._ID)
                        .append("!=?");
                selectionArgs[i++] = "" + c.getLong(0);
            }
            int cnt = db.delete(Item.TABLE_NAME, b.toString(), selectionArgs);
            if (DEBUG) Log.d(TAG, "Pruned " + cnt + " items");

            return cnt;
        } catch (SQLiteException e) {
            // We can safely ignore this and just prune next time
        }
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        if (sortOrder == null) {
            sortOrder = Item.COUNT + " DESC";
        }

        int which = sUriMatcher.match(uri);
        switch(which) {
        case MATCH_ITEMS:
            qb.setTables(Item.TABLE_NAME);
            qb.setProjectionMap(sFilesProjectionMap);
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri " + uri);
        }

        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            degrade(db);
            prune(db);
            db.setTransactionSuccessful();
        } catch(SQLiteException e) {
            // Do nothing
        } finally {
            if (db != null) db.endTransaction();
        }

        db = mOpenHelper.getWritableDatabase();

        Cursor c = qb.query(
                db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
                "" + MAX_RESULTS);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch(sUriMatcher.match(uri)) {
        case MATCH_ITEMS:
            return Item.CONTENT_TYPE;
        default:
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int which = sUriMatcher.match(uri);
        String table;
        switch(which) {
        case MATCH_ITEMS:
            table = Item.TABLE_NAME;
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long id = db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            SQLiteStatement stmt = db.compileStatement(STMT_UPDATE_COUNT);
            stmt.bindString(1, values.getAsString(Item.KEY));
            int cnt = stmt.executeUpdateDelete();
            if (cnt > 0) {
                Cursor c = null;
                try {
                    String selection = Item.KEY + "=?";
                    String[] selectionArgs = new String[]{
                            values.getAsString(Item.KEY),
                    };
                    c = db.query(MostFrequentlyUsedContract.Item.TABLE_NAME,
                            new String[]{Item._ID},
                            selection,
                            selectionArgs,
                            null, null, null, null);
                    if (c.moveToFirst()) {
                        id = c.getLong(0);
                    }
                } finally {
                    if (c != null) c.close();
                }
            }
        }
        if (id > -1) {
            mResolver.notifyChange(uri, null);
        }
        return Uri.withAppendedPath(uri, "" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int which = sUriMatcher.match(uri);
        String table;
        switch(which) {
        case MATCH_ITEMS:
            table = MostFrequentlyUsedContract.Item.TABLE_NAME;
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int cnt = db.delete(table, selection, selectionArgs);
        if (cnt > 0) {
            mResolver.notifyChange(uri, null);
        }
        return cnt;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int which = sUriMatcher.match(uri);
        String table;
        switch(which) {
        case MATCH_ITEMS:
            table = Item.TABLE_NAME;
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri " + uri);
        }

        values = new ContentValues(values);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int cnt = db.update(table, values, selection, selectionArgs);
        values.put(Item.COUNT, Item.COUNT + " + 1");
        if (cnt > 0) {
            mResolver.notifyChange(uri, null);
        }
        return cnt;
    }
}
