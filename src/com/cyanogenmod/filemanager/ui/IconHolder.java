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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;

import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.KnownMimeTypeResolver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A class that holds icons for a more efficient access.
 */
public class IconHolder {

    private static final int MAX_CACHE = 500;

    private final Map<String, Drawable> mIcons;     // Themes based
    private final Map<String, Drawable> mAppIcons;  // App based

    private final boolean mUseThumbs;


    /**
     * Constructor of <code>IconHolder</code>.
     *
     * @param useThumbs If thumbs of images, videos, apps, ... should be returned
     * instead of the default icon.
     */
    public IconHolder(boolean useThumbs) {
        super();
        this.mIcons = new HashMap<String, Drawable>();
        this.mUseThumbs = useThumbs;
        this.mAppIcons = new LinkedHashMap<String, Drawable>(MAX_CACHE, .75F, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Entry<String, Drawable> eldest) {
                return size() > MAX_CACHE;
            }
        };
    }

    /**
     * Method that returns a drawable reference of a icon.
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

    /**
     * Method that returns a drawable reference of a FileSystemObject.
     *
     * @param context The current context
     * @param index The index of the element that request the thumb
     * @param fso The FileSystemObject reference
     * @return Drawable The drawable reference
     */
    public Drawable getDrawable(Context context, int index, FileSystemObject fso) {
        Drawable dw = null;
        if (mUseThumbs) {
            // Is cached?
            final String filepath = fso.getFullPath();
            if (this.mAppIcons.containsKey(filepath)) {
                return this.mAppIcons.get(filepath);
            }

            // Retrieve the drawable
            if (KnownMimeTypeResolver.isAndroidApp(context, fso)) {
                dw = getAppDrawable(context, fso);
            } else if (KnownMimeTypeResolver.isImage(context, fso)) {
                dw = getImageDrawable(context, fso);
            } else if (KnownMimeTypeResolver.isVideo(context, fso)) {
                dw = getVideoDrawable(context, fso);
            }

            // Check if we have a drawable
            if (dw != null) {
                // Returns and caches the new drawable
                this.mAppIcons.put(filepath, dw);
            }
        }
        return dw;
    }

    /**
     * Method that returns the main icon of the app
     *
     * @param context The current context
     * @param fso The FileSystemObject
     * @return Drawable The drawable or null if cannot be extracted
     */
    private static Drawable getAppDrawable(Context context, FileSystemObject fso) {
        final String filepath = fso.getFullPath();
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(filepath, PackageManager.GET_ACTIVITIES);
        if (packageInfo != null) {
            // Read http://code.google.com/p/android/issues/detail?id=9151, CM fixed this
            // issue. We retain it for compatibility with older versions and roms without this fix.
            // Required to access apk which are not installed.
            final ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = filepath;
            appInfo.publicSourceDir = filepath;
            return pm.getDrawable(appInfo.packageName, appInfo.icon, appInfo);
        }
        return null;
    }

    /**
     * Method that returns a thumbnail of the picture
     *
     * @param context The current context
     * @param fso The FileSystemObject
     * @return Drawable The drawable or null if cannot be extracted
     */
    private static Drawable getImageDrawable(Context context, FileSystemObject fso) {
        Bitmap thumb = ThumbnailUtils.createImageThumbnail(
                fso.getFullPath(), ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL);
        if (thumb == null) {
            return null;
        }
        return new BitmapDrawable(context.getResources(), thumb);
    }

    /**
     * Method that returns a thumbnail of the picture
     *
     * @param context The current context
     * @param fso The FileSystemObject
     * @return Drawable The drawable or null if cannot be extracted
     */
    private static Drawable getVideoDrawable(Context context, FileSystemObject fso) {
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(
                fso.getFullPath(), ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL);
        if (thumb == null) {
            return null;
        }
        return new BitmapDrawable(context.getResources(), thumb);
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        for (Drawable dw : this.mAppIcons.values()) {
            dw.setCallback(null);
            if (dw instanceof BitmapDrawable) {
                ((BitmapDrawable)dw).getBitmap().recycle();
            }
        }
        this.mAppIcons.clear();
    }
}
