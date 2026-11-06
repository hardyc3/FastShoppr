/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class MainActivitySmokeTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Test
  public void launchesAndNavigatesToPlaceholders() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onView(withText(R.string.app_name)).check(matches(isDisplayed()));

      onView(withId(R.id.get_started_button)).perform(click());
      onView(withText(R.string.lists_title)).check(matches(isDisplayed()));

      pressBack();
      onView(withId(R.id.open_settings_button)).perform(click());
      onView(withText(R.string.settings_title)).check(matches(isDisplayed()));
    }
  }
}
