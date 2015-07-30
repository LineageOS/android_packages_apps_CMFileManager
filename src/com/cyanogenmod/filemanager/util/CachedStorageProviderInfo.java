package com.cyanogenmod.filemanager.util;


import com.cyanogen.ambient.storage.provider.ProviderCapabilities;
import com.cyanogen.ambient.storage.provider.ProviderStatusCodes;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;

public class CachedStorageProviderInfo extends StorageProviderInfo {

    public CachedStorageProviderInfo(String authority, String packageName, String rootDocId,
            String title, String summary, int iconId, int colorId, int flags, int extFlags) {
        mAuthority = authority;
        mPackage = packageName;
        mRootDocumentId = rootDocId;
        mTitle = title;
        mSummary = summary;
        mIconId = iconId;
        mColor = colorId;
        mNeedUserAuth = true;
        mProviderStatus = ProviderStatusCodes.SUCCESS_CACHE;
        mProviderCapabilities = new ProviderCapabilities(flags, extFlags);
    }
}
