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

package com.cyanogenmod.filemanager.util;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.widget.ImageView;
import com.cyanogenmod.filemanager.R;

import java.util.HashMap;
import java.util.Map;

public class CircleIconHelper {
    /*private static Map<Integer, IconData> sIcons = new HashMap<Integer, IconData>();
    private static int mDirectoryColor;
    static {
        setVolumeColor(getResources().getColor(R.color.default_primary));
        loadDefaultIcons();
    }

    private static class IconData {
        ColorStateList iconColor;
        boolean isDir;

        public IconData(ColorStateList iconColor, boolean isDir) {
            this.iconColor = iconColor;
            this.isDir = isDir;
        }
    }

    public static void setIcon(Resources res, ImageView view, int iconId) {
        IconData iconData;
        if (sIcons.containsKey(iconId)) {
            // get predefined colors associated with mimetype iconid
            iconData = sIcons.get(iconId);
        } else {
            // use a default icon based on misc icon and colors
            iconData = sIcons.get(R.drawable.ic_category_misc);
        }

        view.setBackgroundTintList(iconData.iconColor);
        view.setImageResource(iconId);
        view.setColorFilter(res.getColor(R.color.navigation_view_icon_fill), Mode.MULTIPLY);
    }

    *//**
     * Method that loads the default icons (known icons and more common icons).
     *//*
    private static void loadDefaultIcons() {
        loadIcon(R.drawable.ic_category_apps,
                mContext.getResources().getColor(R.color.category_apps), false);
        loadIcon(R.drawable.ic_category_archives,
                mContext.getResources().getColor( R.color.category_archives), false);
        loadIcon(R.drawable.ic_category_audio,
                mContext.getResources().getColor(R.color.category_audio), false);
        loadIcon(R.drawable.ic_category_docs,
                mContext.getResources().getColor(R.color.category_docs), false);
        loadIcon(R.drawable.ic_category_images,
                mContext.getResources().getColor(R.color.category_images), false);
        loadIcon(R.drawable.ic_category_misc,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_category_video,
                mContext.getResources().getColor(R.color.category_video), false);
        loadIcon(R.drawable.ic_filetype_binary,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_font,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_source,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_calendar,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_ebook,
                mContext.getResources().getColor(R.color.category_docs), false);
        loadIcon(R.drawable.ic_filetype_markup,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_spreadsheet,
                mContext.getResources().getColor(R.color.category_docs), false);
        loadIcon(R.drawable.ic_filetype_cdimage,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_email,
                mContext.getResources().getColor(R.color.category_docs), false);
        loadIcon(R.drawable.ic_filetype_pdf,
                mContext.getResources().getColor(R.color.category_docs), false);
        loadIcon(R.drawable.ic_filetype_system_file,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_contact,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_executable,
                mContext.getResources().getColor(R.color.category_misc), false);
        loadIcon(R.drawable.ic_filetype_preso,
                mContext.getResources().getColor(R.color.category_docs), false);
        loadIcon(R.drawable.ic_filetype_text,
                mContext.getResources().getColor(R.color.category_docs), false);

        // Icon selected state
        loadIcon(R.drawable.ic_check,
                mContext.getResources().getColor(R.color.navigation_view_icon_selected), false);
    }

    private static IconData loadIcon(int resId, int color, boolean isDir) {
        //Check if the icon exists in the cache
        if (sIcons.containsKey(resId)) {
            sIcons.remove(resId);
        }

        //Load the drawable, cache and returns reference
        ColorStateList colorList = new ColorStateList(new int[][]{new int[]{}},
                new int[]{color});
        IconData iconData = new IconData(colorList, isDir);
        sIcons.put(resId, iconData);
        return iconData;
    }*/
}
