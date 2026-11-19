/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class SettingsStoreSelectionTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject SelectedStoreRepository selectedStoreRepository;

  @Before
  public void setUp() {
    hiltRule.inject();
    clearSelectedStores();
  }

  @After
  public void tearDown() {
    clearSelectedStores();
  }

  @Test
  public void savesZipAndManualStoreAndShowsSummary() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onFocusedView(withId(R.id.open_settings_button)).perform(click());
      waitFor(() -> onFocusedView(withId(R.id.storeZipInput)).check(matches(isDisplayed())));

      onFocusedView(withId(R.id.storeZipInput)).perform(replaceText("90210"));
      onFocusedView(withId(R.id.btnSaveZip)).perform(click());
      waitFor(
          () ->
              onFocusedView(withId(R.id.storeSummary))
                  .check(matches(withText(containsString("ZIP: 90210")))));

      onFocusedView(withId(R.id.btnEnterStoreManually)).perform(click());
      waitFor(
          () ->
              onFocusedView(withHint(R.string.settings_store_manual_name_hint))
                  .check(matches(isDisplayed())));
      onFocusedView(withHint(R.string.settings_store_manual_name_hint))
          .perform(replaceText("Walmart #123"));
      onFocusedView(withHint(R.string.settings_store_manual_address_hint))
          .perform(replaceText("1 Main St"));
      onFocusedView(withId(android.R.id.button1)).perform(click());

      waitFor(
          () ->
              onFocusedView(withId(R.id.storeSummary))
                  .check(matches(withText(containsString("Walmart: Walmart #123 — 1 Main St")))));

      NearbyStore saved = selectedStoreRepository.getSelected(StoreBrand.WALMART);
      assertNotNull(saved);
      assertEquals("Walmart #123", saved.getName());
      assertEquals("1 Main St", saved.getAddressLine());
      assertEquals(NearbyStoreSource.MANUAL, saved.getSource());
      assertEquals("90210", selectedStoreRepository.getPostalCode());
    }
  }

  private void clearSelectedStores() {
    for (StoreBrand brand : StoreBrand.values()) {
      if (brand != StoreBrand.CUSTOM) {
        selectedStoreRepository.clear(brand);
      }
    }
    selectedStoreRepository.setPostalCode(null);
  }

  private static ViewInteraction onFocusedView(Matcher<View> matcher) {
    return onView(allOf(matcher, inFocusedWindow()));
  }

  private static Matcher<View> inFocusedWindow() {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(View view) {
        return view.hasWindowFocus();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("view is in the focused window");
      }
    };
  }

  private static void waitFor(Runnable assertion) {
    long deadline = SystemClock.uptimeMillis() + 10_000L;
    while (SystemClock.uptimeMillis() < deadline) {
      try {
        assertion.run();
        return;
      } catch (AssertionError | RuntimeException exception) {
        SystemClock.sleep(50L);
      }
    }
    assertion.run();
  }
}
