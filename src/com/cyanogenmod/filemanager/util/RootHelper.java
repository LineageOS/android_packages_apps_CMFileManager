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

package com.cyanogenmod.filemanager.util;

import com.cyanogenmod.filemanager.console.ConsoleBuilder;

/**
 * A helper class with useful funtions for root mode
 */
public final class RootHelper {

    private static final String SHELL_EMULATED = "/storage/emulated";
    private static final String ROOT_EMULATED = "/mnt/shell/emulated";

    /**
     * This method correct the path used by access the emulated card on 4.3+, when root
     * doesn't a zigote process and /storage/emulated is not accessible
     *
     * @param path The path to check
     * @return String The path fixed
     */
    public static String fixRootPaths(String path) {
        // Only on root console, 4.3+ and start with SHELL_EMULATED
        if (!ConsoleBuilder.isPrivileged() ||
            android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 ||
            !path.startsWith(SHELL_EMULATED)) {
            return path;
        }
        return path.replace(SHELL_EMULATED, ROOT_EMULATED);
    }
}
