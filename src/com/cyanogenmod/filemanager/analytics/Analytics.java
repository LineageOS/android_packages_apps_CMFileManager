package com.cyanogenmod.filemanager.analytics;

import android.content.ContentResolver;
import android.content.Context;
import com.cyanogen.ambient.analytics.AnalyticsServices;
import com.cyanogen.ambient.analytics.Event;
import com.cyanogen.ambient.common.api.AmbientApiClient;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

/**
 * Created by herriojr on 8/7/15.
 */
public class Analytics {
    /**
     * Actions
     */
    private static final String ACTION_PROVIDER_ADD = "provider.add";

    private static final String ACTION_PROVIDER_DELETE = "provider.delete";

    private static final String ACTION_PROPERTY = "property";

    private static final String ACTION_NO_APPLICATION_FOR_FILE = "no.application.for.file";

    public static final String ACTION_FILE_CREATE = "file.create";

    public static final String ACTION_FILE_DELETE = "file.delete";

    public static final String ACTION_FILE_OPEN = "file.open";

    public static final String ACTION_FILE_MOVE = "file.move";

    public static final String ACTION_FILE_COPY = "file.copy";

    public static final String ACTION_FILE_SHARE = "file.share";

    public static final String ACTION_FILE_SEARCH = "file.search";

    /**
     * Provider Fields
     */
    private static final String FIELD_PROVIDER_AUTHORITY = "authority";

    private static final String FIELD_PROVIDER_TITLE = "title";

    private static final String FIELD_PROVIDER_STATUS = "status";

    private static final String FIELD_PROVIDER_AVAILABLE_BYTES = "available.bytes";

    /**
     * File Fields
     */
    public static final String FIELD_FILE_MIMETYPE = "mimetype";

    public static final String FIELD_FILE_IS_SECURE = "is.secure";

    public static final String FIELD_FILE_PROVIDER_NAME = "provider.name";

    public static final String FIELD_FILE_LAST_ACCESS_TIME = "last.access.time";

    /**
     * Property Fields
     */
    public static final String FIELD_ROOT_ENABLED = "enabled";

    private Context mContext;

    private final String mCategory;

    private AmbientApiClient mClient;

    public Analytics(Context context, AmbientApiClient client) {
        mContext = context;
        mCategory = context.getPackageName();
        mClient = client;
    }

    private void send(Event.Builder builder) {
        AnalyticsServices.AnalyticsApi.sendEvent(mClient, builder.build());
    }

    private Event.Builder addStorageProviderInfo(Event.Builder builder, StorageProviderInfo info) {
        ContentResolver r;
        return builder
                .addField(FIELD_PROVIDER_AUTHORITY, info.getAuthority())
                .addField(FIELD_PROVIDER_TITLE, info.getTitle())
                .addField(FIELD_PROVIDER_STATUS, info.getProviderStatus())
                .addField(FIELD_PROVIDER_AVAILABLE_BYTES, info.getAvailableBytes());
    }

    private Event.Builder addFileSystemObject(Event.Builder builder, FileSystemObject fso) {
        return builder
                .addField(FIELD_FILE_IS_SECURE, fso.isSecure())
                .addField(FIELD_FILE_MIMETYPE,
                        MimeTypeHelper.getMimeType(mContext, fso))
                .addField(FIELD_FILE_PROVIDER_NAME,
                        StorageApiConsole.getProviderNameFromFullPath(fso.getProviderPrefix()))
                .addField(FIELD_FILE_LAST_ACCESS_TIME, fso.getLastAccessedTime().toGMTString());
    }

    /**
     * Records the connection of a provider with FileManager
     *
     * TODO: Figure out if it is associated with a provider or not.
     *
     * @param info
     */
    public void recordProviderAdded(StorageProviderInfo info) {
        send(addStorageProviderInfo(
                new Event.Builder(
                        mCategory,
                        ACTION_PROVIDER_ADD),
                info));
    }

    public void recordProviderDeleted(StorageProviderInfo info) {
        send(addStorageProviderInfo(
                new Event.Builder(
                        mCategory,
                        ACTION_PROVIDER_DELETE),
                info));
    }

    public void recordActionPerformedOnFile(String action, FileSystemObject fso) {
        send(addFileSystemObject(new Event.Builder(
                        mCategory,
                        action),
                fso));
    }

    public void recordNoApplicationForFile(FileSystemObject fso) {
        send(addFileSystemObject(new Event.Builder(
                        mCategory,
                        ACTION_NO_APPLICATION_FOR_FILE),
                fso));
    }

    public void recordSearchPerformed() {
        send(new Event.Builder(
                mCategory,
                ACTION_FILE_SEARCH
        ));
    }

    public void recordRootAccessEnabled(boolean enabled) {
        send(new Event.Builder(
                        mCategory,
                        ACTION_PROPERTY)
                .addField(FIELD_ROOT_ENABLED, enabled));
    }

    public void recordNotificiationClicked() {

    }
}
