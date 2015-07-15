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

import com.cyanogenmod.filemanager.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.TextView;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;


/**
 * Basic tests showcasing simple view matchers and actions like {@link ViewMatchers#withId},
 * {@link ViewActions#click} and {@link ViewActions#typeText}.
 * <p>
 * Note that there is no need to tell Espresso that a view is in a different {@link Activity}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class WelcomeActivityTest {

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
    public ActivityTestRule<WelcomeActivity> mActivityRule = new ActivityTestRule<>(
            WelcomeActivity.class);

    /**
     * This test goes through the welcome screens and tries clicking connect button.
     */
    @Test
    public void testClickConnectCloud() {
        goThroughWelcomePages();
        onView(withId(R.id.dismiss_card)).perform(click());
        onView(withId(R.id.dismiss_card)).check(doesNotExist());
    }

    /**
     * This test goes back and forth the welcome, verifying texts and buttons.
     */
    @Test
    public void testClickNextOnWelcomePage() {
        goThroughWelcomePages();

        onView(withId(R.id.prevButton)).perform(click());

        // Third page, to go back to the second page
        onView(withId(R.id.nextButton)).check(matches(isDisplayed()));
        onView(withId(R.id.pagination)).check(matches(isDisplayed()));
        onView(withId(R.id.prevButton)).perform(click());

        // Second page, to go back to the first page
        onView(withId(R.id.titleMessageOne)).check(matches(withText(R.string.second_title)));
        onView(withId(R.id.bottomMessageOne)).check(matches(withText(R.string.first_message)));
        onView(withId(R.id.nextButton)).check(matches(isDisplayed()));
        onView(withId(R.id.pagination)).check(matches(isDisplayed()));
        onView(withId(R.id.prevButton)).perform(click());

        goThroughWelcomePages();

        onView(withId(R.id.nextButton)).perform(click());
    }

    /**
     * Helper function.
     */
    private void goThroughWelcomePages() {
        // First page
        onView(withId(R.id.welcome_textview_0)).check(matches(withText(R.string.welcome_to)));
        onView(withId(R.id.welcome_textview_1)).check(matches(withText(R.string.file)));
        onView(withId(R.id.welcome_textview_2)).check(matches(withText(R.string.manager)));
        onView(withId(R.id.welcome_textview_3)).check(matches(withText(R.string.welcome_desc)));
        onView(withId(R.id.prevButton)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)));
        onView(withId(R.id.pagination)).check(matches(isDisplayed()));
        onView(withId(R.id.nextButton)).perform(click());
        // Second page
        onView(withId(R.id.titleMessageOne)).check(matches(withText(R.string.second_title)));
        onView(withId(R.id.bottomMessageOne)).check(matches(withText(R.string.first_message)));
        onView(withId(R.id.prevButton)).check(matches(isDisplayed()));
        onView(withId(R.id.pagination)).check(matches(isDisplayed()));
        onView(withId(R.id.nextButton)).perform(click());
        // Third page
        onView(withId(R.id.prevButton)).check(matches(isDisplayed()));
        onView(withId(R.id.pagination)).check(matches(isDisplayed()));
        onView(withId(R.id.nextButton)).perform(click());
        // Last page
        onView(withId(R.id.dismiss_card)).check(matches(withText(R.string.connect_now)));
        onView(withId(R.id.cardHeaderText)).check(matches(withText(R.string.add_cloud_storage)));
        onView(withId(R.id.cardChildText)).check(matches(withText(R.string.oobe_add_cloud_storage_desc)));
        onView(withId(R.id.nextButton)).check(matches(isDisplayed()));
        onView(withId(R.id.pagination)).check(matches(isDisplayed()));
    }
}
