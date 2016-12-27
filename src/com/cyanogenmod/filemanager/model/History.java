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

package com.cyanogenmod.filemanager.model;

import android.net.Uri;
import android.provider.BaseColumns;

import com.cyanogenmod.filemanager.parcelables.HistoryNavigable;
import com.cyanogenmod.filemanager.providers.BookmarksContentProvider;

import java.io.Serializable;

/**
 * A class that holds the a navigation history information.
 */
public class History implements Serializable, Comparable<History> {

    private static final long serialVersionUID = -8891185225878742265L;

    private int mPosition;
    private final HistoryNavigable mItem;

    /**
     * Constructor of <code>History</code>.
     *
     * @param position The history position
     * @param item The item that holds the history information
     */
    public History(int position, HistoryNavigable item) {
        super();
        this.mPosition = position;
        this.mItem = item;
    }

    /**
     * Columns of the database
     */
    public static class Columns implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
                Uri.parse(String.format(
                        "%s%s/%s", //$NON-NLS-1$
                        "content://", //$NON-NLS-1$
                        BookmarksContentProvider.AUTHORITY,
                        "/history")); //$NON-NLS-1$

        /**
         * The title of the history
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title"; //$NON-NLS-1$

        /**
         * The description of the history
         * <P>Type: TEXT</P>
         */
        public static final String DESCRIPTION = "description"; //$NON-NLS-1$

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER =
                DESCRIPTION + " ASC"; //$NON-NLS-1$

        /**
         * @hide
         */
        public static final String[] HISTORY_QUERY_COLUMNS =
                { _ID, TITLE, DESCRIPTION };

        /**
         * These save calls to cursor.getColumnIndexOrThrow()
         * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
         *
         * @hide
         */
        public static final int HISTORY_ID_INDEX = 0;

        /**
         * @hide
         */
        public static final int HISTORY_TITLE_INDEX = 1;

        /**
         * @hide
         */
        public static final int HISTORY_DESCRIPTION_INDEX = 2;
    }

    /**
     * Method that returns the position of the history.
     *
     * @return int The history position
     */
    public int getPosition() {
        return this.mPosition;
    }

    /**
     * Method that sets the current position of the history
     *
     * @param position The current position
     */
    public void setPosition(int position) {
        this.mPosition = position;
    }

    /**
     * Method that returns the item that holds the history information.
     *
     * @return HistoryNavigable The item that holds the history information
     */
    public HistoryNavigable getItem() {
        return this.mItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.mItem == null) ? 0 : this.mItem.hashCode());
        result = prime * result + this.mPosition;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        History other = (History) obj;
        if (this.mItem == null) {
            if (other.mItem != null) {
                return false;
            }
        } else if (!this.mItem.equals(other.mItem)) {
            return false;
        }
        if (this.mPosition != other.mPosition) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(History another) {
        return Integer.valueOf(this.mPosition).compareTo(Integer.valueOf(another.mPosition));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "History [position=" + this.mPosition //$NON-NLS-1$
                + ", item=" + String.valueOf(this.mItem) + "]";  //$NON-NLS-1$//$NON-NLS-2$
    }

}