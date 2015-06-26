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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.StorageApi.Document;
import com.cyanogen.ambient.storage.StorageApi.Document.DocumentResult;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;

import java.util.LinkedList;
import java.util.List;

/**
 * A helper class with useful methods for dealing with Storage Providers.
 */
public final class StorageProviderUtils {

    public static final String CACHE_DIR = ".storage-provider-files";

    private static final String TAG = StorageProviderUtils.class.getSimpleName();

    public static class PathInfo {
        private String mDisplayName;
        private String mPath;

        public PathInfo(String displayName, String path) {
            mDisplayName = displayName;
            mPath = path;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        public String getPath() {
            return mPath;
        }
    }

    /**
     * Return the Drawable for this Storage Provider
     * @param context
     * @param authority
     * @param icon
     */
    public static Drawable loadPackageIcon(Context context, String authority, int icon) {
        if (icon != 0) {
            if (authority != null) {
                final PackageManager pm = context.getPackageManager();
                final ProviderInfo info = pm.resolveContentProvider(authority, 0);
                if (info != null) {
                    return pm.getDrawable(info.packageName, icon, info.applicationInfo);
                }
            } else {
                return context.getDrawable(icon);
            }
        }
        return null;
    }

    public static List<PathInfo> reconstructStorageApiFilePath(final String file) {
        final LinkedList<PathInfo> pathList = new LinkedList<PathInfo>();

        StorageApiConsole console = StorageApiConsole.getStorageApiConsoleForPath(file);
        if (console != null) {
            StorageApi storageApi = console.getStorageApi();
            final StorageProviderInfo providerInfo = console.getStorageProviderInfo();
            if (storageApi == null) {
                return null;
            } else if (providerInfo == null || TextUtils.isEmpty(file)) {
                return null;
            }
            final int hashCode = StorageApiConsole.getHashCodeFromProvider(providerInfo);
            final String rootId = StorageApiConsole.constructStorageApiFilePathFromProvider(
                    providerInfo.getRootDocumentId(), hashCode);
            String path = StorageApiConsole.getProviderPathFromFullPath(file);

            do {
                PendingResult<DocumentResult> pendingResult =
                        storageApi.getMetadata(providerInfo, path, false);
                DocumentResult documentResult = pendingResult.await();
                if (documentResult == null) {
                    Log.e(TAG, "Result: FAIL. No results returned."); //$NON-NLS-1$
                    break;
                }
                Document document = documentResult.getDocument();
                String documentPath =
                        StorageApiConsole
                                .constructStorageApiFilePathFromProvider(
                                        document.getId(), hashCode);

                String documentName;
                if (TextUtils.equals(rootId, documentPath)) {
                    documentName = providerInfo.getTitle();
                    path = null;
                } else {
                    documentName = document.getDisplayName();
                    path = document.getParentId();

                }

                PathInfo pathInfo;
                pathInfo = new PathInfo(documentName, documentPath);
                pathList.addFirst(pathInfo);
            } while (!TextUtils.isEmpty(path));
        }
        return pathList;
    }
}
