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

package com.cyanogenmod.filemanager.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;

import java.util.List;

public class ProviderAdapter extends BaseAdapter {
    Context mContext;
    List<StorageProviderInfo> mEntries;
    int mIndividualItem;

    public ProviderAdapter(Context context, int individualItem, List<StorageProviderInfo> objects) {
        mContext = context;
        mEntries = objects;
        mIndividualItem = individualItem;
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mEntries.indexOf(getItem(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            LayoutInflater layoutInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = layoutInflater.inflate(mIndividualItem, null);
        }
        StorageProviderInfo spi = (StorageProviderInfo) getItem(position);

        ImageView providerIcon = (ImageView) convertView.findViewById(R.id.providerIcon);
        TextView providerTitle = (TextView) convertView.findViewById(R.id.providerTitle);
        TextView providerSummary = (TextView) convertView.findViewById(R.id.providerUsername);

        providerIcon.setImageDrawable(getIcon(spi.getPackage()));
        providerTitle.setText(spi.getTitle());
        providerSummary.setText(spi.getSummary());

        return convertView;
    }

    private Drawable getIcon(String packageName) {
        try {
            return mContext.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
