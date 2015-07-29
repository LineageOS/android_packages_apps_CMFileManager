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
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import android.widget.ImageView.ScaleType;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.RootDirectory;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MediaHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.KnownMimeTypeResolver;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;
import java.util.Map;

/**
 * A class that holds icons for a more efficient access.
 */
public class IconHolder {

    private static final int MAX_CACHE = 500;

    private static final int MSG_LOAD = 1;
    private static final int MSG_LOADED = 2;

    private final Map<Integer, IconData> mIcons;     // Themes based
    private final Map<String, Drawable> mAppIcons;  // App based

    private Map<String, Long> mAlbums;      // Media albums

    private final WeakHashMap<ImageView, Loadable> mRequests;

    private final Context mContext;
    private final boolean mUseThumbs;

    private int mDirectoryColor;

    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    private ViewOutlineProvider mIconViewOutlineProvider;

    private static class IconData {
        ColorStateList iconColor;
        boolean isDir;

        public IconData(ColorStateList iconColor, boolean isDir) {
            this.iconColor = iconColor;
            this.isDir = isDir;
        }
    }

    /**
     * This is kind of a hack, we should have a loadable for each MimeType we run into.
     * TODO: Refactor this to have different loadables
     */
    private static class Loadable {
        private Context mContext;
        private static boolean sAlbumsDirty = true;
        private static Map<String, Long> sAlbums;

        FileSystemObject fso;
        WeakReference<ImageView> view;
        Drawable result;

        public Loadable(Context context, ImageView view, FileSystemObject fso) {
            this.mContext = context.getApplicationContext();
            this.fso = fso;
            this.view = new WeakReference<ImageView>(view);
            this.result = null;
        }

        private static synchronized Map<String, Long> getAlbums(Context context) {
            if (sAlbumsDirty) {
                sAlbums = MediaHelper.getAllAlbums(context.getContentResolver());
                sAlbumsDirty = false;
            }
            return sAlbums;
        }

        public static synchronized void dirtyAlbums() {
            sAlbumsDirty = true;
        }

        public boolean load() {
            return (result = loadDrawable(fso)) != null;
        }

        private Drawable loadDrawable(FileSystemObject fso) {
            final String filePath = MediaHelper.normalizeMediaPath(fso.getFullPath());

            if (fso instanceof RootDirectory) {
                return getRootDrawable(fso);
            }
            if (KnownMimeTypeResolver.isAndroidApp(mContext, fso)) {
                return getAppDrawable(fso);
            } else if (KnownMimeTypeResolver.isImage(mContext, fso)) {
                return getImageDrawable(filePath);
            } else if (KnownMimeTypeResolver.isVideo(mContext, fso)) {
                return getVideoDrawable(filePath);
            } else if (FileHelper.isDirectory(fso)) {
                Map<String, Long> albums = getAlbums(mContext);
                if (albums.containsKey(filePath)) {
                    return getAlbumDrawable(albums.get(filePath));
                }
            }
            return null;
        }

        /**
         * Method that returns the main icon of the root
         *
         * @param fso The FileSystemObject
         * @return Drawable The drawable or null if cannot be extracted
         */
        private Drawable getRootDrawable(FileSystemObject fso) {
            final StorageApiConsole console = StorageApiConsole.getStorageApiConsoleForPath(
                    ((RootDirectory) fso).getRootPath());
            if (console != null && console.getStorageProviderInfo() != null) {
                final StorageProviderInfo providerInfo = console.getStorageProviderInfo();
                Drawable icon = StorageProviderUtils.loadPackageIcon(mContext,
                        providerInfo.getAuthority(), providerInfo.getIcon());
                return icon;
            }
            return null;
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
            PackageInfo packageInfo = pm.getPackageArchiveInfo(filepath,
                    PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                // Read http://code.google.com/p/android/issues/detail?id=9151, CM fixed this
                // issue. We retain it for compatibility with older versions and roms without
                // this fix. Required to access apk which are not installed.
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
                    Images.Thumbnails.MINI_KIND);
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
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOADED:
                    processResult((Loadable) msg.obj);
                    break;
            }
        }

        private void processResult(Loadable result) {
            ImageView view = result.view.get();
            if (view == null) {
                return;
            }

            Loadable requestedForImageView = mRequests.get(view);
            if (requestedForImageView != result) {
                return;
            }

            // Cache the new drawable
            final String filePath = MediaHelper.normalizeMediaPath(result.fso.getFullPath());
            if (result.result != null) {
                mAppIcons.put(filePath, result.result);
            }
            if (!(result.fso instanceof RootDirectory)) {
                view.setBackground(null);
                view.setColorFilter(null);
            } else {
                view.setColorFilter(
                        mContext.getResources().getColor(R.color.navigation_view_icon_fill),
                        Mode.SRC_IN);
                view.setScaleType(ScaleType.CENTER);
            }
            view.setImageDrawable(result.result);
        }
    };

    private ContentObserver mMediaObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Loadable.dirtyAlbums();
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
        this.mRequests = new WeakHashMap<ImageView, Loadable>();
        this.mIcons = new HashMap<Integer, IconData>();
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

        mIconViewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = (int)mContext.getResources().getDimension(R.dimen.circle_icon_wh);
                int radius =
                        (int)mContext.getResources().getDimension(R.dimen.rectangle_icon_radius);
                outline.setRoundRect(0, 0, size, size, radius);
            }
        };

        setVolumeColor(mContext.getResources().getColor(R.color.default_primary));
        loadDefaultIcons();
    }

    /**
     * Method that returns a drawable reference of a FileSystemObject.
     *
     * @param iconView View to load the drawable into
     * @param fso The FileSystemObject reference
     * @param defaultIcon Drawable to be used in case no specific one could be found
     * @return Drawable The drawable reference
     */
    public void loadDrawable(ImageView iconView, FileSystemObject fso, int defaultIcon) {
        IconData iconData;
        iconView.setOutlineProvider(mIconViewOutlineProvider);
        iconView.setClipToOutline(true);
        boolean selectedThumbnail = (mUseThumbs && fso != null &&
                TextUtils.isEmpty(fso.getProviderPrefix()) && defaultIcon == R.drawable.ic_check &&
                mAppIcons.containsKey(MediaHelper.normalizeMediaPath(fso.getFullPath())));
        // TODO: implement code to get thumbnail from storage providers, until then force default
        if ((mUseThumbs && fso != null && TextUtils.isEmpty(fso.getProviderPrefix())
                && !selectedThumbnail) || ( fso != null && fso instanceof RootDirectory &&
                !TextUtils.isEmpty(fso.getProviderPrefix()))) {
            // Is cached?
            final String filePath = MediaHelper.normalizeMediaPath(fso.getFullPath());
            if (this.mAppIcons.containsKey(filePath)) {
                if (fso instanceof RootDirectory) {
                    iconData = mIcons.get(defaultIcon);
                    iconView.setBackgroundResource(R.drawable.ic_icon_background);
                    iconView.setBackgroundTintList(iconData.iconColor);
                    iconView.setImageDrawable(this.mAppIcons.get(filePath));
                    iconView.setColorFilter(
                            mContext.getResources().getColor(R.color.navigation_view_icon_fill),
                            Mode.SRC_IN);
                } else {
                    iconView.setBackground(null);
                    iconView.setColorFilter(null);
                    iconView.setImageDrawable(this.mAppIcons.get(filePath));
                }
                return;
            }

            if (mWorkerThread == null) {
                mWorkerThread = new HandlerThread("IconHolderLoader");
                mWorkerThread.start();
                mWorkerHandler = new WorkerHandler(mWorkerThread.getLooper());
            }
            Loadable previousForView = mRequests.get(iconView);
            if (previousForView != null) {
                mWorkerHandler.removeMessages(MSG_LOAD, previousForView);
            }

            Loadable loadable = new Loadable(mContext, iconView, fso);
            mRequests.put(iconView, loadable);

            mWorkerHandler.obtainMessage(MSG_LOAD, loadable).sendToTarget();
        }

        if (mIcons.containsKey(defaultIcon)) {
            iconData = mIcons.get(defaultIcon);
        } else {
            int primaryColor;
            if (fso != null && fso instanceof RootDirectory) {
                int iconResId = fso.getResourceIconId();
                primaryColor = ((RootDirectory) fso).getPrimaryColor();
                if (iconResId > 0) {
                    defaultIcon = iconResId;
                }
            } else {
                primaryColor = mContext.getResources().getColor(R.color.category_misc);
            }
            iconData = loadIcon(defaultIcon, primaryColor, false);
        }

        int color = iconData.isDir ? mDirectoryColor :
                mContext.getResources().getColor(R.color.navigation_view_icon_fill);

        if (selectedThumbnail) {
            // if thumbnail
            iconView.setBackgroundTintList(null);
            iconView.setBackgroundColor(
                    mContext.getResources().getColor(R.color.navigation_view_icon_selected));
        } else {
            // if not thumbnail
            iconView.setBackgroundResource(R.drawable.ic_icon_background);
            iconView.setBackgroundTintList(iconData.iconColor);
        }

        iconView.setImageResource(defaultIcon);
        iconView.setColorFilter(color, Mode.MULTIPLY);
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD:
                    Loadable l = (Loadable) msg.obj;
                    if (l.load()) {
                        mHandler.obtainMessage(MSG_LOADED, l).sendToTarget();
                    }
                    break;
            }
        }
    }

    /**
     * Shut down worker thread
     */
    private void shutdownWorker() {
        if (mWorkerThread != null) {
            mWorkerThread.getLooper().quit();
            mWorkerHandler = null;
            mWorkerThread = null;
        }
    }

    /**
     * Free any resources used by this instance
     */
    public void cleanup() {
        this.mRequests.clear();
        this.mIcons.clear();
        this.mAppIcons.clear();
        mContext.getContentResolver().unregisterContentObserver(mMediaObserver);
        shutdownWorker();
    }

    /**
     * Method that sets the colors to use for the current volumes directories
     */
    public void setVolumeColor(int color) {
        mDirectoryColor = color;
        float opacity =
                mContext.getResources().getFloat(R.float_type.navigation_view_icon_circle_opacity);
        int transparentColor = Color.argb(
                Math.round(((float)0xFF) * opacity),
                Color.red(color),
                Color.green(color),
                Color.blue(color));
        loadIcon(R.drawable.ic_folder, transparentColor, true);
    }

    /**
     * Method that loads the default icons (known icons and more common icons).
     */
    private void loadDefaultIcons() {
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

    private IconData loadIcon(int resId, int color, boolean isDir) {
        //Check if the icon exists in the cache
        if (mIcons.containsKey(resId)) {
            mIcons.remove(resId);
        }

        //Load the drawable, cache and returns reference
        ColorStateList colorList = new ColorStateList(new int[][]{new int[]{}},
                new int[]{color});
        IconData iconData = new IconData(colorList, isDir);
        mIcons.put(resId, iconData);
        return iconData;
    }
}
