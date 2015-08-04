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

package com.cyanogenmod.filemanager.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.cyanogen.ambient.storage.provider.StorageContract;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

public class CloudProviderReceiver extends BroadcastReceiver {

    private static final String TAG = "FMCloudReceiver";
    private static final boolean DEBUG = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.v(TAG, "Got Provider update");
        Bundle bundle = intent.getExtras();
        StorageProviderInfo provider =
                (StorageProviderInfo)bundle.get(StorageContract.EXTRA_PROVIDER);
        boolean loggedIn = bundle.getBoolean(StorageContract.EXTRA_PROVIDER_LOGIN);

        if (DEBUG) Log.v(TAG, "Logging In: " + loggedIn);
        if (loggedIn) {
            // we have logged in, add a provider
            StorageProviderUtils.addProvider(context.getApplicationContext(), provider);
        } else {
            // we logged out, remove that provider
            StorageProviderUtils.removeProvider(context.getApplicationContext(), provider);
        }
    }
}
