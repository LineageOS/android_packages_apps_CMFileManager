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

import android.app.Activity;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;

import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.util.StorageHelper;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.Test;

import java.io.InvalidClassException;
import java.lang.InterruptedException;
import java.util.List;
import java.util.Random;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * The timeouts for each test size is:
 * @SmallTest  - 1 minute
 * @MediumTest - 3 minutes
 * @LargeTest  - 5 minutes
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MainActivityTest {
    private static final String TAG = MainActivityTest.class.getSimpleName();
    private static final String OK = "OK";
    private static final String YES = "Yes";
    private static final String NEW_FOLDER = "New folder";
    private static final String NEW_FILE = "New file";
    private static final String DELETE_SELECTION = "Delete selection";
    private static final String COPY_TO = "Copy to";
    private static final String MOVE_TO = "Move to";
    private static final String DROPBOX = "Dropbox";
    private static final String LOCAL_STORAGE = "Local storage";
    private static final String COPY = "Copy";
    private static final String MOVE = "Move";
    private static final String MORE_OPTIONS = "More options";

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
        pauseForProviderToLoad();
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
        mActivity.navigateToPath(path);
        Thread.sleep(1000);
        Assert.assertTrue("Current Fragment is not NAVIGATION",
                mActivity.isCurrentFragment(MainActivity.FragmentType.NAVIGATION));
    }

    /**
     * Tests dismissing add cloud storage card.
     */
    @Test
    public void dismissAddCloudStorageCard() throws InterruptedException, InvalidClassException {
        onView(withId(R.id.dismiss_card)).check(matches(isDisplayed()));
        onView(withId(R.id.dismiss_card)).perform(click());
        onView(withId(R.id.dismiss_card)).check(matches(not(isDisplayed())));
    }

    /**
     * Tests creating local folder and file.
     */
    @Test
    public void createDeleteLocalFolderAndFile() throws InterruptedException {
        String FOLDER_NAME = "BTestFolder" + new Random().nextInt();
        String FILE_NAME = "TestFile" + new Random().nextInt();

        mActivity.navigateToPath(StorageHelper.getLocalStoragePath(mActivity));

        // create a folder
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FOLDER)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FOLDER_NAME));
        onView(withText(OK)).perform(click());

        // check folder exists
        onView(withId(R.id.navigation_view_layout))
                .check(matches(withAdaptedData(withDocumentName(FOLDER_NAME))));

        // create a file
        onData(withDocumentName(FOLDER_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(click());
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FILE)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FILE_NAME));
        onView(withText(OK)).perform(click());

        // check file exists
        onView(withId(R.id.navigation_view_layout))
                .check(matches(withAdaptedData(withDocumentName(FILE_NAME))));

        Thread.sleep(2000);
        pressBack();
        Thread.sleep(2000);

        // delete the folder
        onData(withDocumentName(FOLDER_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(longClick());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(DELETE_SELECTION)).perform(click());
        onView(withText(YES)).perform(click());

        // check folder is gone
        onView(withId(R.id.navigation_view_layout))
                .check(matches(not(withAdaptedData(withDocumentName(FOLDER_NAME)))));
    }

    /**
     * Tests copying a file from local to Dropbox
     */
    @Test
    public void copyFileFromLocalToDropbox() {
        String FILE_NAME = "LocalFile" + new Random().nextInt();
        mActivity.navigateToPath(StorageHelper.getLocalStoragePath(mActivity));

        // create a file
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FILE)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FILE_NAME));
        onView(withText(OK)).perform(click());
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));

        // long press the file
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(longClick());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(COPY_TO)).perform(click());
        onView(withText(DROPBOX)).perform(click());
        onView(withText(COPY)).perform(click());

        // Check file in Dropbox
        List<StorageProviderInfo> providerInfoList = mActivity.getProviderList();
        StorageProviderInfo storageProviderInfo = providerInfoList.get(0);
        String path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                storageProviderInfo.getRootDocumentId(),
                StorageApiConsole.getHashCodeFromProvider(storageProviderInfo));
        mActivity.navigateToPath(path);
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(click());
    }

    /**
     * Tests moving a file from local to Dropbox
     */
    @Test
    public void moveFileFromLocalToDropbox() {
        String FILE_NAME = "LocalFile" + new Random().nextInt();
        mActivity.navigateToPath(StorageHelper.getLocalStoragePath(mActivity));

        // create a file
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FILE)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FILE_NAME));
        onView(withText(OK)).perform(click());
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));

        // long press the file
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(longClick());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(MOVE_TO)).perform(click());
        onView(withText(DROPBOX)).perform(click());
        onView(withText(MOVE)).perform(click());

        // Check not in local storage
        onView(withId(R.id.navigation_view_layout))
                .check(matches(not(withAdaptedData(withDocumentName(FILE_NAME)))));

        // Check file in Dropbox
        List<StorageProviderInfo> providerInfoList = mActivity.getProviderList();
        StorageProviderInfo storageProviderInfo = providerInfoList.get(0);
        String path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                storageProviderInfo.getRootDocumentId(),
                StorageApiConsole.getHashCodeFromProvider(storageProviderInfo));
        mActivity.navigateToPath(path);
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(click());
    }

    /**
     * Tests copying a file from local to Dropbox
     */
    @Test
    public void copyFileFromDropboxToLocal() throws InterruptedException {
        String FILE_NAME = "DropboxFile" + new Random().nextInt();

        pauseForProviderToLoad();
        List<StorageProviderInfo> providerInfoList = mActivity.getProviderList();
        StorageProviderInfo storageProviderInfo = providerInfoList.get(0);
        String path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                storageProviderInfo.getRootDocumentId(),
                StorageApiConsole.getHashCodeFromProvider(storageProviderInfo));
        mActivity.navigateToPath(path);

        // create a file
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FILE)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FILE_NAME));
        onView(withText(OK)).perform(click());
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));

        // long press the file
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(longClick());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(COPY_TO)).perform(click());
        onView(withText(LOCAL_STORAGE)).perform(click());
        onView(withText(COPY)).perform(click());

        // Check file in local storage
        mActivity.navigateToPath(StorageHelper.getLocalStoragePath(mActivity));
        pauseForProviderToLoad();
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests moving a file from Dropbox to local
     */
    @Test
    public void moveFileFromDropboxToLocal() throws InterruptedException {
        String FILE_NAME = "DropboxFile" + new Random().nextInt();

        pauseForProviderToLoad();
        List<StorageProviderInfo> providerInfoList = mActivity.getProviderList();
        StorageProviderInfo storageProviderInfo = providerInfoList.get(0);
        String path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                storageProviderInfo.getRootDocumentId(),
                StorageApiConsole.getHashCodeFromProvider(storageProviderInfo));
        mActivity.navigateToPath(path);

        // create a file
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FILE)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FILE_NAME));
        onView(withText(OK)).perform(click());
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));

        // long press the file
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(longClick());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(MOVE_TO)).perform(click());
        onView(withText(LOCAL_STORAGE)).perform(click());
        onView(withText(MOVE)).perform(click());

        // Check not in Dropbox
        onView(withId(R.id.navigation_view_layout))
                .check(matches(not(withAdaptedData(withDocumentName(FILE_NAME)))));

        // Check file in local storage
        mActivity.navigateToPath(StorageHelper.getLocalStoragePath(mActivity));
        pauseForProviderToLoad();
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests moving a file from Dropbox to local
     */
    @Test
    public void createDeleteFileAndFolderinDropbox() throws InterruptedException {
        String FILE_NAME = "DropboxFile" + new Random().nextInt();
        String FOLDER_NAME = "DropboxFile" + new Random().nextInt();

        // open Dropbox Navigation View
        pauseForProviderToLoad();
        List<StorageProviderInfo> providerInfoList = mActivity.getProviderList();
        StorageProviderInfo storageProviderInfo = providerInfoList.get(0);
        String path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                storageProviderInfo.getRootDocumentId(),
                StorageApiConsole.getHashCodeFromProvider(storageProviderInfo));
        mActivity.navigateToPath(path);

        // create a file
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FILE)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FILE_NAME));
        onView(withText(OK)).perform(click());
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));

        // create a folder
        onView(withContentDescription(MORE_OPTIONS)).perform(click());
        onView(withText(NEW_FOLDER)).perform(click());
        onView(withId(R.id.input_name_dialog_edit)).perform(clearText()).perform(typeText(FOLDER_NAME));
        onView(withText(OK)).perform(click());
        onData(withDocumentName(FOLDER_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));

        // move the file into the folder
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(longClick());
        onView(withId(R.id.ab_actions)).perform(click());
        onView(withText(MOVE_TO)).perform(click());
        onView(withText(DROPBOX)).perform(click());
        onData(withDocumentName(FOLDER_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(click());
        onView(withText(MOVE)).perform(click());

        // check the file is not in root folder
        onView(withId(R.id.navigation_view_layout))
                .check(matches(not(withAdaptedData(withDocumentName(FILE_NAME)))));

        // check the file is moved into the folder
        onData(withDocumentName(FOLDER_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .perform(click());
        onData(withDocumentName(FILE_NAME))
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
                .check(matches(isDisplayed()));
    }

    /**
     * Returns a Matcher<Object> from a document name which matches FileSystemObject.getName()
     * @param name
     * @return Matcher<Object>
     */
    public static Matcher<Object> withDocumentName(final String name) {
        return new BoundedMatcher<Object, FileSystemObject>(FileSystemObject.class) {

            @Override
            protected boolean matchesSafely(FileSystemObject systemObj) {
                return name.equals(systemObj.getName());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with name: " + name);
            }
        };
    }

    /**
     * Returns a Matcher<View> asserting that a data item is not in an adapter
     * @param dataMatcher
     * @return Matcher<View>
     */
    private static Matcher<View> withAdaptedData(final Matcher<Object> dataMatcher) {
        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("with class name: ");
                dataMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof AdapterView)) {
                    return false;
                }

                @SuppressWarnings("rawtypes")
                Adapter adapter = ((AdapterView) view).getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (dataMatcher.matches(adapter.getItem(i))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private void pauseForProviderToLoad() throws InterruptedException {
        Thread.sleep(2000); // need time for Activity to initialize getProviderList()
    }
}
