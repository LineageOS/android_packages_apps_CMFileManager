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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationSortMode;
import com.cyanogenmod.filemanager.preferences.ObjectIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;

public class SortViewOptions extends LinearLayout {
    private Drawable mSelectedShape;
    private int mSelectedTextColor;
    private int mUnselectedTextColor;

    private IconGroup mGroupABC;
    private IconGroup mGroupDate;
    private IconGroup mGroupSize;
    private IconGroup mGroupType;
    private IconGroup mGroupAsc;
    private IconGroup mGroupDesc;

    private IconGroup[] mTopGroup;
    private IconGroup[] mBottomGroup;

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

    public SortViewOptions(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public SortViewOptions(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mSelectedShape = getResources().getDrawable(R.drawable.ic_sort_selector);
        mSelectedTextColor = getResources().getColor(R.color.darker_black);
        mUnselectedTextColor = getResources().getColor(R.color.lighter_black);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        boolean asc = true;
        int defaultId = ((ObjectIdentifier) FileManagerSettings.SETTINGS_SORT_MODE
                .getDefaultValue()).getId();
        int selected = Preferences.getSharedPreferences()
                .getInt(FileManagerSettings.SETTINGS_SORT_MODE.getId(), defaultId);
        NavigationSortMode selectedSortMode = NavigationSortMode.fromId(selected);

        mGroupABC = setupIcon(this, R.id.sort_item_abc, R.drawable.ic_sort_abc,
                R.string.sort_by_name, NavigationSortMode.NAME_ASC, NavigationSortMode.NAME_DESC);
        mGroupDate = setupIcon(this, R.id.sort_item_date, R.drawable.ic_sort_date,
                R.string.sort_by_date, NavigationSortMode.DATE_ASC, NavigationSortMode.DATE_DESC);
        mGroupSize = setupIcon(this, R.id.sort_item_size, R.drawable.ic_sort_size,
                R.string.sort_by_size, NavigationSortMode.SIZE_ASC, NavigationSortMode.SIZE_DESC);
        mGroupType = setupIcon(this, R.id.sort_item_type, R.drawable.ic_sort_type,
                R.string.sort_by_type, NavigationSortMode.TYPE_ASC, NavigationSortMode.TYPE_DESC);

        mTopGroup = new IconGroup[] {
                mGroupABC,
                mGroupDate,
                mGroupSize,
                mGroupType,
        };

        final GroupClickListener topClickListener = new GroupClickListener(mTopGroup);
        for (IconGroup ig : mTopGroup) {
            ig.mGroup.setOnClickListener(topClickListener);
            if (ig.mAsc == selectedSortMode || ig.mDesc == selectedSortMode) {
                asc = (ig.mAsc == selectedSortMode);
                ig.setSelected(true);
            }
        }

        mGroupAsc = setupIcon(this, R.id.sort_item_asc, R.drawable.ic_sort_asc,
                R.string.sort_by_asc, null, null);
        mGroupDesc = setupIcon(this, R.id.sort_item_desc, R.drawable.ic_sort_desc,
                R.string.sort_by_desc, null, null);

        mBottomGroup = new IconGroup[] {
                mGroupAsc,
                mGroupDesc,
        };

        final GroupClickListener botClickListener = new GroupClickListener(mBottomGroup);
        for (IconGroup ig : mBottomGroup) {
            ig.mGroup.setOnClickListener(botClickListener);
        }

        mBottomGroup[asc ? 0 : 1].setSelected(true);
    }

    private IconGroup setupIcon(View view, int groupId, int imageDrawableId, int textId,
                                NavigationSortMode asc, NavigationSortMode desc) {
        IconGroup iconGroup = new IconGroup();
        iconGroup.mGroup = view.findViewById(groupId);
        iconGroup.mIcon = (ImageView)iconGroup.mGroup.findViewById(R.id.sort_item_icon);
        iconGroup.mText = (TextView)iconGroup.mGroup.findViewById(R.id.sort_item_title);

        iconGroup.mIcon.setImageDrawable(getResources().getDrawable(imageDrawableId));
        iconGroup.mText.setText(getResources().getString(textId));

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
}
