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

package com.cyanogenmod.filemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.WelcomeAdapter;
import com.cyanogenmod.filemanager.views.CirclePageIndicator;
import com.cyanogenmod.filemanager.views.PageIndicator;
import com.cyanogenmod.filemanager.views.InkPageIndicator;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        final WelcomeAdapter mSectionsPagerAdapter =
                new WelcomeAdapter(getSupportFragmentManager());
        AppCompatButton mFinishBtn = (AppCompatButton) findViewById(R.id.intro_btn_finish);
        InkPageIndicator inkPageIndicator = (InkPageIndicator) findViewById(R.id.indicator);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);

        if (mViewPager != null && inkPageIndicator != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mViewPager.setCurrentItem(0);
            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                                           int positionOffsetPixels) {
                }
                @Override
                public void onPageSelected(int position) {
                }
                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
            inkPageIndicator.setViewPager(mViewPager);
            if (mFinishBtn != null) {
                mFinishBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing
    }
}
