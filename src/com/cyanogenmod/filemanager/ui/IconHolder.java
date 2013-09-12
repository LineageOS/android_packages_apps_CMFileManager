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

package com.cyanogenmod.filemanager.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that holds icons for a more efficient access.
 */
public class IconHolder {

    private final Map<String, Drawable> mIcons;

    /**
     * Constructor of <code>IconHolder</code>.
     */
    public IconHolder() {
        super();
        this.mIcons = new HashMap<String, Drawable>();
    }

    /**
     * Method that loads, cache and returns a drawable reference
     * of a icon.
     *
     * @param context The current context
     * @param resid The resource identifier
     * @return Drawable The drawable icon reference
     */
    public Drawable getDrawable(Context context, final String resid) {
        //Check if the icon exists in the cache
        if (this.mIcons.containsKey(resid)) {
            return this.mIcons.get(resid);
        }

        //Load the drawable, cache and returns reference
        Theme theme = ThemeManager.getCurrentTheme(context);
        Drawable dw = theme.getDrawable(context, resid);
        this.mIcons.put(resid, dw);
        return dw;
    }

    public Drawable getDrawable(Context context, FileSystemObject fso) {
        String filepath = fso.getFullPath();
        String mime = MimeTypeHelper.getMimeType(context, fso);
        if (TextUtils.equals(mime, "application/vnd.android.package-archive")) {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(filepath,
                    PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                appInfo.sourceDir = filepath;
                appInfo.publicSourceDir = filepath;
                Drawable dw = pm.getDrawable(appInfo.packageName, appInfo.icon, appInfo);
                if (dw != null) {
                    return dw;
                }
            }
        }
        String resid = MimeTypeHelper.getIcon(context, fso);
        return getDrawable(context, resid);
    }


}
