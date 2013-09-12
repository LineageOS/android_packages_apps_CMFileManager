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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;

import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MediaHelper;
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

    private Map<String, Long> mAlbums;      // Media albums

    private final Context mContext;
    private final boolean mUseThumbs;
    private boolean mNeedAlbumUpdate = true;

    private ContentObserver mMediaObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            synchronized (this) {
                mNeedAlbumUpdate = true;
            }
        }
    };

    /**
     * Constructor of <code>IconHolder</code>.
     *
     * @param useThumbs If thumbs of images, videos, apps, ... should be returned
     * instead of the default icon.
     */
    public IconHolder(Context context, boolean useThumbs) {
        super();
        this.mContext = context;
        this.mUseThumbs = useThumbs;
        this.mIcons = new HashMap<String, Drawable>();
        this.mAppIcons = new LinkedHashMap<String, Drawable>(MAX_CACHE, .75F, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Entry<String, Drawable> eldest) {
                return size() > MAX_CACHE;
            }
        };
        this.mAlbums = new HashMap<String, Long>();
        if (useThumbs) {
            final ContentResolver cr = mContext.getContentResolver();
            for (Uri uri : MediaHelper.RELEVANT_URIS) {
                cr.registerContentObserver(uri, true, mMediaObserver);
            }
        }
    }

    /**
     * Method that returns a drawable reference of a icon.
     *
     * @param resid The resource identifier
     * @return Drawable The drawable icon reference
     */
    public Drawable getDrawable(final String resid) {
        //Check if the icon exists in the cache
        if (this.mIcons.containsKey(resid)) {
            return this.mIcons.get(resid);
        }

        //Load the drawable, cache and returns reference
        Theme theme = ThemeManager.getCurrentTheme(mContext);
        Drawable dw = theme.getDrawable(mContext, resid);
        this.mIcons.put(resid, dw);
        return dw;
    }

    /**
     * Method that returns a drawable reference of a FileSystemObject.
     *
     * @param index The index of the element that request the thumb
     * @param fso The FileSystemObject reference
     * @return Drawable The drawable reference
     */
    public Drawable getDrawable(int index, FileSystemObject fso) {
        Drawable dw = null;
        if (mUseThumbs) {
            // Is cached?
            String filepath = fso.getFullPath();
            if (this.mAppIcons.containsKey(filepath)) {
                return this.mAppIcons.get(filepath);
            }

            // Retrieve the drawable
            if (KnownMimeTypeResolver.isAndroidApp(mContext, fso)) {
                dw = getAppDrawable(fso);
            } else if (KnownMimeTypeResolver.isImage(mContext, fso)) {
                filepath = MediaHelper.normalizeMediaPath(filepath);
                dw = getImageDrawable(filepath);
            } else if (KnownMimeTypeResolver.isVideo(mContext, fso)) {
                filepath = MediaHelper.normalizeMediaPath(filepath);
                dw = getVideoDrawable(filepath);
            } else if (FileHelper.isDirectory(fso)) {
                // Fso need to be normalized to match the album path
                filepath = MediaHelper.normalizeMediaPath(filepath);
                synchronized (mMediaObserver) {
                    if (mNeedAlbumUpdate) {
                        mNeedAlbumUpdate = false;
                        this.mAlbums = MediaHelper.getAllAlbums(mContext.getContentResolver());
                    }
                }
                if (this.mAlbums.containsKey(filepath)) {
                    dw = getAlbumDrawable(this.mAlbums.get(filepath));
                }
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
     * @param fso The FileSystemObject
     * @return Drawable The drawable or null if cannot be extracted
     */
    private Drawable getAppDrawable(FileSystemObject fso) {
        final String filepath = fso.getFullPath();
        PackageManager pm = mContext.getPackageManager();
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
     * @param file The path to the file
     * @return Drawable The drawable or null if cannot be extracted
     */
    private Drawable getImageDrawable(String file) {
        Bitmap thumb = ThumbnailUtils.createImageThumbnail(
                MediaHelper.normalizeMediaPath(file),
                ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL);
        if (thumb == null) {
            return null;
        }
        return new BitmapDrawable(mContext.getResources(), thumb);
    }

    /**
     * Method that returns a thumbnail of the video
     *
     * @param file The path to the file
     * @return Drawable The drawable or null if cannot be extracted
     */
    private Drawable getVideoDrawable(String file) {
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(
                MediaHelper.normalizeMediaPath(file),
                ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL);
        if (thumb == null) {
            return null;
        }
        return new BitmapDrawable(mContext.getResources(), thumb);
    }

    /**
     * Method that returns a thumbnail of the album folder
     *
     * @param albumId The album identifier
     * @return Drawable The drawable or null if cannot be extracted
     */
    private Drawable getAlbumDrawable(long albumId) {
        String path = MediaHelper.getAlbumThumbnailPath(mContext.getContentResolver(), albumId);
        if (path == null) {
            return null;
        }
        Bitmap thumb = ThumbnailUtils.createImageThumbnail(path,
                ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL);
        if (thumb == null) {
            return null;
        }
        return new BitmapDrawable(mContext.getResources(), thumb);
    }

    /**
     * Free any resources used by this instance
     */
    public void cleanup() {
        this.mIcons.clear();
        this.mAppIcons.clear();
        mContext.getContentResolver().unregisterContentObserver(mMediaObserver);
    }
}
