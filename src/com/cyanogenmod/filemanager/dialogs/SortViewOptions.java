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
package com.cyanogenmod.filemanager.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.preferences.PreferenceHelper;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationSortMode;
import com.cyanogenmod.filemanager.util.DialogHelper;

public class SortViewOptions {
    private Drawable mSelectedShape;
    private int mSelectedTextColor;
    private int mUnselectedTextColor;

    private IconGroup mGroupRelevance;
    private IconGroup mGroupABC;
    private IconGroup mGroupDate;
    private IconGroup mGroupSize;
    private IconGroup mGroupType;
    private IconGroup mGroupAsc;
    private IconGroup mGroupDesc;

    private IconGroup[] mTopGroup;
    private IconGroup[] mBottomGroup;

    private Context mContext;

    public abstract static class OnClickListener {
        public abstract void onClick(DialogInterface dialog, int which, int result);
    };

    /**
     * Holds points to the different views and sort types for an icon group
     * so that when the user selects it, we can quickly change the ui
     */
    private class IconGroup {
        public View mGroup;
        public ImageView mIcon;
        public TextView mText;
        public NavigationSortMode mAsc;
        public NavigationSortMode mDesc;
        public boolean mSelected;

        public void setSelected(boolean selected) {
            if (selected) {
                mIcon.setBackground(mSelectedShape);
                mIcon.setColorFilter(null);
                mText.setTextColor(mSelectedTextColor);
                mSelected = true;
            } else {
                mIcon.setBackground(null);
                mIcon.setColorFilter(R.color.lighter_black);
                mText.setTextColor(mUnselectedTextColor);
                mSelected = false;
            }
        }
    }

    /**
     * Group click listener than deselects all other items when an item is clicked
     */
    private class GroupClickListener implements View.OnClickListener {
        private IconGroup[] mIconGroup;
        public GroupClickListener(IconGroup[] iconGroup) {
            mIconGroup = iconGroup;
        }

        @Override
        public void onClick(View v) {
            for (IconGroup ig : mIconGroup) {
                ig.setSelected(ig.mGroup == v);
            }
        }
    };

    public SortViewOptions(Context ctx, View parent, FileManagerSettings settingType) {
        mContext = ctx;
        mSelectedShape = mContext.getResources().getDrawable(R.drawable.ic_sort_selector);
        mSelectedTextColor = mContext.getResources().getColor(R.color.darker_black);
        mUnselectedTextColor = mContext.getResources().getColor(R.color.lighter_black);

        init(parent);
        setSettingType(settingType);
    }

    /**
     * Sets the setting type of the dialog - this changes the options the user can choose from
     * @param settingType {@link FileManagerSettings} type
     */
    private void setSettingType(FileManagerSettings settingType) {
        if (settingType == FileManagerSettings.SETTINGS_SORT_MODE) {
            mGroupRelevance.mGroup.setVisibility(View.GONE);
        } else if (settingType == FileManagerSettings.SETTINGS_SORT_SEARCH_RESULTS_MODE) {
            mGroupDate.mGroup.setVisibility(View.GONE);
            mGroupSize.mGroup.setVisibility(View.GONE);
        }  else {
            throw new IllegalArgumentException("Unsupported setting type");
        }

        boolean ascending = true;
        int selected = PreferenceHelper.getIntPreference(settingType);
        NavigationSortMode selectedSortMode = NavigationSortMode.fromId(selected);

        for (IconGroup ig : mTopGroup) {
            if (ig.mAsc == selectedSortMode || ig.mDesc == selectedSortMode) {
                ascending = (ig.mAsc == selectedSortMode);
                ig.setSelected(true);
                break;
            }
        }

        if (ascending) {
            mGroupAsc.setSelected(true);
        } else {
            mGroupDesc.setSelected(true);
        }
    }

    private void init(View parent) {
        mGroupRelevance = setupIcon(parent, R.id.sort_item_relevance, R.drawable.ic_sort_relevance,
                R.string.sort_by_relevance, NavigationSortMode.SEARCH_RELEVANCE_ASC,
                NavigationSortMode.SEARCH_RELEVANCE_DESC);
        mGroupABC = setupIcon(parent, R.id.sort_item_abc, R.drawable.ic_sort_abc,
                R.string.sort_by_name, NavigationSortMode.NAME_ASC, NavigationSortMode.NAME_DESC);
        mGroupDate = setupIcon(parent, R.id.sort_item_date, R.drawable.ic_sort_date,
                R.string.sort_by_date, NavigationSortMode.DATE_ASC, NavigationSortMode.DATE_DESC);
        mGroupSize = setupIcon(parent, R.id.sort_item_size, R.drawable.ic_sort_size,
                R.string.sort_by_size, NavigationSortMode.SIZE_ASC, NavigationSortMode.SIZE_DESC);
        mGroupType = setupIcon(parent, R.id.sort_item_type, R.drawable.ic_sort_type,
                R.string.sort_by_type, NavigationSortMode.TYPE_ASC, NavigationSortMode.TYPE_DESC);

        mTopGroup = new IconGroup[] {
                mGroupRelevance,
                mGroupABC,
                mGroupDate,
                mGroupSize,
                mGroupType,
        };

        final GroupClickListener topClickListener = new GroupClickListener(mTopGroup);
        for (IconGroup ig : mTopGroup) {
            ig.mGroup.setOnClickListener(topClickListener);
        }

        mGroupAsc = setupIcon(parent, R.id.sort_item_asc, R.drawable.ic_sort_asc,
                R.string.sort_by_asc, null, null);
        mGroupDesc = setupIcon(parent, R.id.sort_item_desc, R.drawable.ic_sort_desc,
                R.string.sort_by_desc, null, null);

        mBottomGroup = new IconGroup[] {
                mGroupAsc,
                mGroupDesc,
        };

        final GroupClickListener botClickListener = new GroupClickListener(mBottomGroup);
        for (IconGroup ig : mBottomGroup) {
            ig.mGroup.setOnClickListener(botClickListener);
        }
    }

    private IconGroup setupIcon(View view, int groupId, int imageDrawableId, int textId,
                                NavigationSortMode asc, NavigationSortMode desc) {
        IconGroup iconGroup = new IconGroup();
        iconGroup.mGroup = view.findViewById(groupId);
        iconGroup.mIcon = (ImageView)iconGroup.mGroup.findViewById(R.id.sort_item_icon);
        iconGroup.mText = (TextView)iconGroup.mGroup.findViewById(R.id.sort_item_title);

        iconGroup.mIcon.setImageDrawable(mContext.getResources().getDrawable(imageDrawableId));
        iconGroup.mText.setText(mContext.getResources().getString(textId));

        iconGroup.mAsc = asc;
        iconGroup.mDesc = desc;

        // default the group to false first
        iconGroup.setSelected(false);

        return iconGroup;
    }

    /**
     * @return the id of the NavigationSortMode that the user selected
     */
    public int getSortId() {
        boolean ascending = mGroupAsc.mSelected;
        for (IconGroup ig : mTopGroup) {
            if (ig.mSelected) {
                return ascending ? ig.mAsc.getId() : ig.mDesc.getId();
            }
        }

        return NavigationSortMode.NAME_ASC.getId();
    }

    public static AlertDialog createSortDialog(Context context, FileManagerSettings setting,
                                               final SortViewOptions.OnClickListener listener) {
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = li.inflate(R.layout.sort_view_options, null);
        final SortViewOptions sortViewOptions = new SortViewOptions(context, layout, setting);
        return DialogHelper.createTwoButtonsDialog(context,
                R.string.ok, R.string.cancel, 0, context.getString(R.string.sort_options),
                layout, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onClick(dialog, which, sortViewOptions.getSortId());
                    }
                });
    }
}
