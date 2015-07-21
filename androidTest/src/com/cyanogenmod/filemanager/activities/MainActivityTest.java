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

import android.support.v4.app.Fragment;
import android.util.Log;
import android.app.Activity;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.TextView;
import android.widget.ListView;

import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.util.StorageHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.InterruptedException;
import java.lang.System;
import java.lang.Thread;
import java.util.List;
import java.util.Random;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Basic tests showcasing simple view matchers and actions like {@link ViewMatchers#withId},
 * {@link ViewActions#click} and {@link ViewActions#typeText}.
 * <p>
 * Note that there is no need to tell Espresso that a view is in a different {@link Activity}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MainActivityTest {
    private static final String TAG = MainActivityTest.class.getSimpleName();
    private MainActivity mActivity;

    /**
     * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
     * for {@link ActivityInstrumentationTestCase2}.
     * <p>
     * Rules are interceptors which are executed for each test method and will run before
     * any of your setup code in the {@link Before @Before} method.
     * <p>
     * {@link ActivityTestRule} will create and launch of the activity for you and also expose
     * the activity under test. To get a reference to the activity you can use
     * the {@link ActivityTestRule#getActivity()} method.
     */
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Before
    public void setUp() {
        mActivity = (MainActivity) mActivityRule.getActivity();
        Assert.assertNotNull(mActivity);
    }
    /**
     * Tests the Navigation Fragment of the Main Activity.
     * Navigates to Storage API provider. Verify the provider's title is Dropbox.
     * Verify the navigation fragment is presented.
     */
    @Test
    public void navigateToDropbox() throws InterruptedException {
        Thread.sleep(2000); // need time for Activity to initialize getProviderList()
        List<StorageProviderInfo> providerInfoList = mActivity.getProviderList();
        Assert.assertNotNull("providerInfoList is null", providerInfoList);
        Assert.assertEquals("No longer support single provider. Modify test.",
                providerInfoList.size(), 1);
        StorageProviderInfo storageProviderInfo = providerInfoList.get(0);
        Assert.assertFalse("Provider still needs authentication", storageProviderInfo.needAuthentication());
        Assert.assertEquals("Not equal to Dropbox", storageProviderInfo.getTitle(), "Dropbox");
        String path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                storageProviderInfo.getRootDocumentId(),
                StorageApiConsole.getHashCodeFromProvider(storageProviderInfo));
        Log.d(TAG, "testShowMainActivity: " + path); // path looks like -1672407857:///
        mActivity.navigateToPath(path);
        Thread.sleep(1000);
        Assert.assertTrue("Current Fragment is not NAVIGATION",
                mActivity.isCurrentFragment(MainActivity.FragmentType.NAVIGATION));
    }

    /**
     * Tests dismissing add cloud storage card.
     * On home screen click the Dismiss button. Go to local storage and come back home screen to check if the card
     * has been dismissed.
     */
    @Test
    public void dismissAddCloudStorageCard() {
        String NAVIGATE_UP = "Navigate up";
        String HOME = "Home";
        onView(withId(R.id.dismiss_card)).check(matches(isDisplayed()));
        onView(withId(R.id.dismiss_card)).perform(click()).check(matches(not(isDisplayed())));
        mActivity.navigateToPath(StorageHelper.getLocalStoragePath(mActivity));
        onView(withContentDescription(NAVIGATE_UP)).perform(click());
        onView(withText(HOME)).perform(click());
        onView(withId(R.id.dismiss_card)).check(matches(not(isDisplayed())));
    }

    /**
     * Tests creating local folder and file.
     */
    @Test
    public void createLocalFolderAndFile() {
        String OK = "OK";
        String YES = "Yes";
        String NEW_FOLDER = "New folder";
        String NEW_FILE = "New file";
        String DELETE_SELECTION = "Delete selection";
        String FOLDER_NAME = "TestFolder" + new Random().nextInt();
        String FILE_NAME = "TestFile" + new Random().nextInt();

        mActivity.navigateToPath(StorageHelper.getLocalStoragePath(mActivity));

        // create a folder
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(NEW_FOLDER)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FOLDER_NAME));
        onView(withText(OK)).perform(click());

        // create a file
        onView(withText(FOLDER_NAME)).perform(click());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(NEW_FILE)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FILE_NAME));
        onView(withText(OK)).perform(click());
        onView(withText(FILE_NAME)).check(matches(isDisplayed()));

        pressBack();

        // delete the folder
        onView(withText(FOLDER_NAME)).perform(longClick());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(DELETE_SELECTION)).perform(click());
        onView(withText(YES)).perform(click());

        onView(withText(FOLDER_NAME)).check(doesNotExist());
    }
}
