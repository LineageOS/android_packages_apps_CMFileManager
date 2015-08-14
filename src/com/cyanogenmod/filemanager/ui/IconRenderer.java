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
package com.cyanogenmod.filemanager.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

public class IconRenderer implements IconHolder.ICallback {
    private Context mContext;

    private int mIconId;

    private ViewOutlineProvider mIconViewOutlineProvider;

    public IconRenderer(Context context, int iconId) {
        mContext = context.getApplicationContext();
        mIconId = iconId;
        mIconViewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Resources res = mContext.getResources();
                int size = res.getDimensionPixelSize(R.dimen.circle_icon_wh);
                float radius = res.getDimension(R.dimen.rectangle_icon_radius);
                outline.setRoundRect(0, 0, size, size, radius);
            }
        };
    }

    private static void setIcon(Resources resources, ImageView view, Drawable iconDrawable,
                                int iconColor, int backgroundId, int backgroundColor) {
        StateListDrawable stateListDrawable = new StateListDrawable();
//        addSelected(resources, stateListDrawable);
        addUnselected(stateListDrawable, iconDrawable, iconColor);

        ColorStateList colorList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_selected},
                        new int[]{}},
                new int[]{resources.getColor(R.color.navigation_view_icon_selected),
                        backgroundColor});

        view.setBackgroundResource(backgroundId);
        view.setBackgroundTintList(colorList);
        view.setImageDrawable(stateListDrawable);
    }

    private static void addUnselected(StateListDrawable drawable, Drawable iconDrawable,
                                      int color) {
        iconDrawable.setTint(color);
        drawable.addState(new int[0], iconDrawable);
    }

    private static void addUnselectedThumbnail(StateListDrawable drawable, Drawable iconDrawable) {
        drawable.addState(new int[0], iconDrawable);
    }

    private void setIconThumbnail(Resources resources, ImageView view, Drawable iconDrawable) {
        StateListDrawable stateListDrawable = new StateListDrawable();
//        addSelected(resources, stateListDrawable);
        addUnselectedThumbnail(stateListDrawable, iconDrawable);

        ColorStateList colorList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_selected},
                        new int[]{}},
                new int[]{resources.getColor(R.color.navigation_view_icon_selected),
                        resources.getColor(R.color.navigation_view_icon_unselected)});

        view.setBackgroundResource(R.drawable.ic_icon_background_rounded_rectagle);
        view.setBackgroundTintList(colorList);
        view.setImageDrawable(stateListDrawable);
    }

    @Override
    public void onPreExecute(ImageView imageView) {
        imageView.setOutlineProvider(mIconViewOutlineProvider);
        imageView.setClipToOutline(true);
    }

    @Override
    public void onLoaded(ImageView imageView, Drawable icon) {
        Resources res = mContext.getResources();
        if (icon == null) {
            // Icon holder didn't have anything at the moment, set default.
            int colorId = MimeTypeHelper.getIconColorFromIconId(mContext, mIconId);
            setIcon(res, imageView, res.getDrawable(mIconId),
                    res.getColor(R.color.navigation_view_icon_unselected),
                    R.drawable.ic_icon_background,
                    res.getColor(colorId));
        } else {
            // Thumbnail present, set the background to rectangle to match better.
            setIconThumbnail(res, imageView, icon);
        }
    }
}
