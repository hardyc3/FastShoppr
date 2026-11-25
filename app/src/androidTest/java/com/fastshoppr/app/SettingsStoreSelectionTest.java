/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
  public void savedBranchesAppearInSummaryWithoutZip() {
    selectedStoreRepository.select(branch(StoreBrand.WALMART, "Walmart #123", "1 Main St"));
    selectedStoreRepository.select(branch(StoreBrand.HOME_DEPOT, "Home Depot #456", "2 Oak Ave"));

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onFocusedView(withId(R.id.open_settings_button)).perform(click());
      waitFor(
          () ->
              onFocusedView(withId(R.id.storeSummary))
                  .check(matches(withText(containsString("Walmart #123 — 1 Main St")))));

      onFocusedView(withId(R.id.storeSummary))
          .check(matches(withText(containsString("Home Depot #456 — 2 Oak Ave"))));
      onFocusedView(withId(R.id.storeSummary))
          .check(matches(withText(org.hamcrest.Matchers.not(containsString("ZIP")))));
      onFocusedView(withText(containsString("No ZIP code"))).check(matches(isDisplayed()));

      NearbyStore saved = selectedStoreRepository.getSelected(StoreBrand.WALMART);
      assertNotNull(saved);
      assertNull(saved.getPostalCode());
    }
  }

  @Test
  public void brandPickerControlsUseCurrentBrandsAndExplainPrivacy() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onFocusedView(withId(R.id.open_settings_button)).perform(click());
      waitFor(
          () ->
              onFocusedView(
                      allOf(
                          withId(R.id.storeBrandSpinner),
                          withContentDescription(R.string.settings_store_brand_label)))
                  .check(matches(isDisplayed())));

      onFocusedView(withId(R.id.storeBrandSpinner)).perform(click());
      onFocusedView(withText(R.string.brand_home_depot)).perform(click());

      onFocusedView(allOf(withId(R.id.btnPickBranch), withText(R.string.settings_store_find)))
          .check(matches(isDisplayed()));
      onFocusedView(withText(containsString("No ZIP code"))).check(matches(isDisplayed()));
    }
  }

  @Test
  public void forgetDialogRemovesOnlySelectedBranch() {
    selectedStoreRepository.select(branch(StoreBrand.WALMART, "Walmart #123", "1 Main St"));
    selectedStoreRepository.select(branch(StoreBrand.HOME_DEPOT, "Home Depot #456", "2 Oak Ave"));

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onFocusedView(withId(R.id.open_settings_button)).perform(click());
      waitFor(() -> onFocusedView(withId(R.id.btnForgetSavedStore)).check(matches(isDisplayed())));

      onFocusedView(withId(R.id.btnForgetSavedStore)).perform(click());
      waitFor(
          () ->
              onFocusedView(withText(R.string.settings_store_forget_title))
                  .check(matches(isDisplayed())));
      onFocusedView(withText(containsString("Walmart #123"))).perform(click());
      onFocusedView(withText(R.string.settings_store_remove)).perform(click());

      waitFor(
          () -> {
            assertNull(selectedStoreRepository.getSelected(StoreBrand.WALMART));
            onFocusedView(withId(R.id.storeSummary))
                .check(matches(withText(containsString("Home Depot #456"))));
          });
      assertNotNull(selectedStoreRepository.getSelected(StoreBrand.HOME_DEPOT));
      onFocusedView(withId(R.id.storeSummary))
          .check(matches(withText(org.hamcrest.Matchers.not(containsString("Walmart #123")))));
    }
  }

  private void clearSelectedStores() {
    for (StoreBrand brand : StoreBrand.values()) {
      if (brand != StoreBrand.CUSTOM) {
        selectedStoreRepository.clear(brand);
      }
    }
  }

  private static NearbyStore branch(StoreBrand brand, String name, String address) {
    return new NearbyStore(
        "branch-" + brand.name(),
        brand,
        name,
        address,
        "Denver",
        "CO",
        null,
        null,
        null,
        NearbyStoreSource.LOCATOR);
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
    TestWait.waitFor(assertion, 10_000L);
  }
}
