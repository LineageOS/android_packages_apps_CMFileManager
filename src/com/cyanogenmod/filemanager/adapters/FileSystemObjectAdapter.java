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

package com.cyanogenmod.filemanager.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * An implementation of {@link ArrayAdapter} for display file system objects.
 */
public class FileSystemObjectAdapter
    extends ArrayAdapter<FileSystemObject> implements OnClickListener {

    /**
     * An interface to communicate selection changes events.
     */
    public interface OnSelectionChangedListener {
        /**
         * Method invoked when the selection changed.
         *
         * @param selectedItems The new selected items
         */
        void onSelectionChanged(List<FileSystemObject> selectedItems);
    }

    /**
     * A class that conforms with the ViewHolder pattern to performance
     * the list view rendering.
     */
    private static class ViewHolder {
        /**
         * @hide
         */
        public ViewHolder() {
            super();
        }
        ImageButton mBtCheck;
        ImageView mIvIcon;
        TextView mTvName;
        TextView mTvSummary;
        TextView mTvSize;
        Boolean mHasSelectedBg;
    }

    private IconHolder mIconHolder;
    private final int mItemViewResourceId;
    private HashSet<FileSystemObject> mSelectedItems;
    private final boolean mPickable;

    private OnSelectionChangedListener mOnSelectionChangedListener;

    //The resource of the item check
    private static final int RESOURCE_ITEM_CHECK = R.id.navigation_view_item_check;
    //The resource of the item icon
    private static final int RESOURCE_ITEM_ICON = R.id.navigation_view_item_icon;
    //The resource of the item name
    private static final int RESOURCE_ITEM_NAME = R.id.navigation_view_item_name;
    //The resource of the item summary information
    private static final int RESOURCE_ITEM_SUMMARY = R.id.navigation_view_item_summary;
    //The resource of the item size information
    private static final int RESOURCE_ITEM_SIZE = R.id.navigation_view_item_size;

    /**
     * Constructor of <code>FileSystemObjectAdapter</code>.
     *
     * @param context The current context
     * @param files The list of file system objects
     * @param itemViewResourceId The identifier of the layout that represents an item
     * of the list adapter
     * @param pickable If the adapter should act as a pickable browser.
     */
    public FileSystemObjectAdapter(
            Context context, List<FileSystemObject> files,
            int itemViewResourceId, boolean pickable) {
        super(context, RESOURCE_ITEM_NAME, files);
        this.mItemViewResourceId = itemViewResourceId;
        this.mSelectedItems = new HashSet<FileSystemObject>();
        this.mPickable = pickable;
        notifyThemeChanged(); // Reload icons
    }

    /**
     * Method that sets the listener which communicates selection changes.
     *
     * @param onSelectionChangedListener The listener reference
     */
    public void setOnSelectionChangedListener(
            OnSelectionChangedListener onSelectionChangedListener) {
        this.mOnSelectionChangedListener = onSelectionChangedListener;
    }

    /**
     * Method that loads the default icons (known icons and more common icons).
     */
    private void loadDefaultIcons() {
        this.mIconHolder.getDrawable("ic_fso_folder_drawable"); //$NON-NLS-1$
        this.mIconHolder.getDrawable("ic_fso_default_drawable"); //$NON-NLS-1$
    }

    /**
     * Method that Clears Cache of the adapter
     * @param fso The Selected FileSystemObject reference
     */
    public void clearCache(FileSystemObject fso) {
        if (mIconHolder != null) {
            mIconHolder.clearCacheImages(fso);
            notifyDataSetChanged();
        }
    }

    /**
     * Method that dispose the elements of the adapter.
     */
    public void dispose() {
        clear();
        if (mIconHolder != null) {
            mIconHolder.cleanup();
            mIconHolder = null;
        }
        this.mSelectedItems.clear();
    }

    /**
     * Method that returns the {@link FileSystemObject} reference from his path.
     *
     * @param path The path of the file system object
     * @return FileSystemObject The file system object reference
     */
    public FileSystemObject getItem(String path) {
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
          //File system object info
            FileSystemObject fso = getItem(i);
            if (fso.getFullPath().compareTo(path) == 0) {
                return fso;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Check to reuse view
        View v = convertView;
        Theme theme = ThemeManager.getCurrentTheme(getContext());

        if (v == null) {
            //Create the view holder
            LayoutInflater li =
                    (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(this.mItemViewResourceId, parent, false);
            ViewHolder viewHolder = new FileSystemObjectAdapter.ViewHolder();
            viewHolder.mIvIcon = (ImageView)v.findViewById(RESOURCE_ITEM_ICON);
            viewHolder.mTvName = (TextView)v.findViewById(RESOURCE_ITEM_NAME);
            viewHolder.mTvSummary = (TextView)v.findViewById(RESOURCE_ITEM_SUMMARY);
            viewHolder.mTvSize = (TextView)v.findViewById(RESOURCE_ITEM_SIZE);
            if (!this.mPickable) {
                viewHolder.mBtCheck = (ImageButton)v.findViewById(RESOURCE_ITEM_CHECK);
                viewHolder.mBtCheck.setOnClickListener(this);
            } else {
                viewHolder.mBtCheck = (ImageButton)v.findViewById(RESOURCE_ITEM_CHECK);
                viewHolder.mBtCheck.setVisibility(View.GONE);
            }
            v.setTag(viewHolder);
        }

        //Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder)v.getTag();
        if (this.mPickable) {
            theme.setBackgroundDrawable(getContext(), v, "background_drawable"); //$NON-NLS-1$
        }

        FileSystemObject fso = getItem(position);

        Drawable dwIcon = this.mIconHolder.getDrawable(
                MimeTypeHelper.getIcon(getContext(), fso, true));
        mIconHolder.loadDrawable(viewHolder.mIvIcon, fso, dwIcon);

        viewHolder.mTvName.setText(fso.getName());
        theme.setTextColor(getContext(), viewHolder.mTvName, "text_color"); //$NON-NLS-1$
        if (viewHolder.mTvSummary != null) {
            StringBuilder sbSummary = new StringBuilder();
            if (fso instanceof ParentDirectory) {
                sbSummary.append(getContext().getResources().getString(R.string.parent_dir));
            } else {
                sbSummary.append(
                        FileHelper.formatFileTime(
                                getContext(), fso.getLastModifiedTime()));
                sbSummary.append("   "); //$NON-NLS-1$
                sbSummary.append(fso.toRawPermissionString());
            }
            viewHolder.mTvSummary.setText(sbSummary);
            theme.setTextColor(getContext(), viewHolder.mTvSummary, "text_color"); //$NON-NLS-1$
        }
        if (viewHolder.mTvSize != null) {
            viewHolder.mTvSize.setText(FileHelper.getHumanReadableSize(fso));
            theme.setTextColor(getContext(), viewHolder.mTvSize, "text_color"); //$NON-NLS-1$
        }
        if (!this.mPickable) {
            viewHolder.mBtCheck.setVisibility(
                    TextUtils.equals(fso.getName(), FileHelper.PARENT_DIRECTORY) ?
                            View.INVISIBLE : View.VISIBLE);

            boolean selected = mSelectedItems.contains(fso);
            Drawable dwCheck;
            if (selected) {
                dwCheck = theme.getDrawable(getContext(), "checkbox_selected_drawable"); //$NON-NLS-1$
            } else {
                dwCheck = theme.getDrawable(getContext(), "checkbox_deselected_drawable"); //$NON-NLS-1$
            }
            viewHolder.mBtCheck.setImageDrawable(dwCheck);
            viewHolder.mBtCheck.setTag(position);

            if (viewHolder.mHasSelectedBg == null
                    || viewHolder.mHasSelectedBg != selected) {
                String drawableId = selected
                        ? "selectors_selected_drawable" //$NON-NLS-1$
                        : "selectors_deselected_drawable"; //$NON-NLS-1$

                theme.setBackgroundDrawable(getContext(), v, drawableId);
                viewHolder.mHasSelectedBg = selected;
            }
        }

        //Return the view
        return v;
    }

    /**
     * Method that returns if the item of the passed position is selected.
     *
     * @param position The position of the item
     * @return boolean If the item of the passed position is selected
     */
    public boolean isSelected(int position) {
        return mSelectedItems.contains(getItem(position));
    }

    /**
     * Method that selects in the {@link ArrayAdapter} the passed item.
     *
     * @param fso The file system object to select
     */
    public void toggleSelection(FileSystemObject fso) {
        toggleSelection(null, fso);
    }

    /**
     * Method that selects in the {@link ArrayAdapter} the passed item.
     *
     * @param v The check view object (can be null)
     * @param fso The file system object to select
     */
    private void toggleSelection(View v, FileSystemObject fso) {
        Theme theme = ThemeManager.getCurrentTheme(getContext());

        boolean selected = !mSelectedItems.remove(fso);
        if (selected) {
            mSelectedItems.add(fso);
        }
        if (v != null) {
            ((View)v.getParent()).setSelected(selected);
        }

        //Communicate event
        if (this.mOnSelectionChangedListener != null) {
            this.mOnSelectionChangedListener.onSelectionChanged(
                    new ArrayList<FileSystemObject>(mSelectedItems));
        }

        notifyDataSetChanged();
    }

    /**
     * Method that deselect all items.
     */
    public void deselectedAll() {
        this.mSelectedItems.clear();
        doSelectDeselectAllVisibleItems(false);
    }

    /**
     * Method that select all visible items.
     */
    public void selectedAllVisibleItems() {
        doSelectDeselectAllVisibleItems(true);
    }

    /**
     * Method that deselect all visible items.
     */
    public void deselectedAllVisibleItems() {
        doSelectDeselectAllVisibleItems(false);
    }

    /**
     * Method that select/deselect all items.
     *
     * @param select Indicates if select (true) or deselect (false) all items.
     */
    private void doSelectDeselectAllVisibleItems(boolean select) {
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = getItem(i);
            if (fso.getName().compareTo(FileHelper.PARENT_DIRECTORY) == 0) {
                // No select the parent directory
                continue;
            }
            if (select) {
                mSelectedItems.add(fso);
            } else {
                mSelectedItems.remove(fso);
            }
        }

        //Communicate event
        if (this.mOnSelectionChangedListener != null) {
            this.mOnSelectionChangedListener.onSelectionChanged(
                    new ArrayList<FileSystemObject>(mSelectedItems));
        }

        notifyDataSetChanged();
    }

    /**
     * Method that returns the selected items.
     *
     * @return List<FileSystemObject> The selected items
     */
    public List<FileSystemObject> getSelectedItems() {
        return new ArrayList<FileSystemObject>(this.mSelectedItems);
    }

    /**
     * Method that sets the selected items.
     *
     * @param selectedItems The selected items
     */
    public void setSelectedItems(List<FileSystemObject> selectedItems) {
        mSelectedItems.clear();
        mSelectedItems.addAll(selectedItems);
        notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {

        //Select or deselect the item
        int pos = ((Integer)v.getTag()).intValue();

        if (pos >= getCount() || pos < 0) {
            return;
        }

        //Retrieve data holder
        final FileSystemObject fso = getItem(pos);

        //What button was pressed?
        switch (v.getId()) {
            case RESOURCE_ITEM_CHECK:
                //Get the row item view
                toggleSelection(v, fso);
                break;
            default:
                break;
        }
    }

    /**
     * Method that should be invoked when the theme of the app was changed
     */
    public void notifyThemeChanged() {
        // Empty icon holder
        if (this.mIconHolder != null) {
            this.mIconHolder.cleanup();
        }
        final boolean displayThumbs = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_DISPLAY_THUMBS.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_DISPLAY_THUMBS.getDefaultValue()).booleanValue());
        this.mIconHolder = new IconHolder(getContext(), displayThumbs);
        loadDefaultIcons();
    }

}
