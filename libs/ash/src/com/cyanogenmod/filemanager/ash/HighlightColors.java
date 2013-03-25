/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.ash;

import android.graphics.Color;

/**
 * An enumeration of all the color resources available for syntax highlight processors.
 */
public enum HighlightColors {

    /**
     * Normal text color
     */
    NORMAL("ash_normal_color", Color.argb(153, 0, 0, 0)),
    /**
     * Assignment text color
     */
    ASSIGMENT("ash_assigment_color", Color.argb(153, 0, 0, 0)),
    /**
     * Single line comment color
     */
    SINGLE_LINE_COMMENT("ash_single_line_comment_color", Color.argb(255, 63, 127, 95)),
    /**
     * Multiline line comment color
     */
    MULTILINE_LINE_COMMENT("ash_multiline_line_comment_color", Color.argb(255, 127, 159, 191)),
    /**
     * Keyword color
     */
    KEYWORD("ash_keyword_color", Color.argb(255, 127, 0, 85)),
    /**
     * String color
     */
    STRING("ash_string_color", Color.argb(255, 42, 0, 255)),
    /**
     * Variable color
     */
    VARIABLE("ash_variable_color", Color.argb(153, 0, 0, 192)); 


    private final String mResId;
    private final int mDefault;

    /**
     * Constructor of <code>HighlightColors</code>
     */
    HighlightColors(String resid, int def) {
        mResId = resid;
        mDefault = def;
    }

    /**
     * Returns the resource identifier
     *
     * @return String The resource identifier
     */
    public String getResId() {
        return this.mResId;
    }

    /**
     * Returns the default value
     *
     * @return String The default value
     */
    public int getDefault() {
        return this.mDefault;
    }

}
