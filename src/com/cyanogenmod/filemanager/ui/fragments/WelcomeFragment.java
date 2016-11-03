/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;

public class WelcomeFragment extends Fragment {

    public WelcomeFragment() {
    }

    public static WelcomeFragment newInstance(int sectionNumber) {
        WelcomeFragment fragment = new WelcomeFragment();
        Bundle args = new Bundle();
        args.putInt("section_number", sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater mInflater, ViewGroup mContainer,
            Bundle mSavedInstance) {
        int mPosition = getArguments().getInt("section_number");
        View mView = mInflater.inflate(R.layout.fragment_intro_content, mContainer, false);
        TextView mTitle = (TextView) mView.findViewById(R.id.benefits_title);
        TextView mDescription = (TextView) mView.findViewById(R.id.benefits_message);
        ImageView mImage = (ImageView) mView.findViewById(R.id.benefits_img);

        switch (mPosition) {
            case 1:
                mTitle.setText(getString(R.string.slide0_title));
                mDescription.setText(getString(R.string.slide0_message));
                mImage.setImageResource(R.drawable.img_oobe_files);
                break;
            case 2:
                mTitle.setText(getString(R.string.slide1_title));
                mDescription.setText(getString(R.string.slide1_message));
                mImage.setImageResource(R.drawable.img_oobe_privacy);
                break;
            case 3:
                mTitle.setText(getString(R.string.slide2_title));
                mDescription.setText(getString(R.string.slide2_message));
                mImage.setImageResource(R.drawable.img_oobe_root);
                break;
        }
        return mView;
    }
}
