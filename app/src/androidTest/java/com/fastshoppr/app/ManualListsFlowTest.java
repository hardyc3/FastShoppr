/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.preferences.SettingsPreferences;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorPages;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class ManualListsFlowTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;

  @Inject SettingsPreferences settingsPreferences;

  @Inject StoreRepository storeRepository;

  @Inject SelectedStoreRepository selectedStoreRepository;

  @Before
  public void setUp() {
    hiltRule.inject();
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM aisle_cache");
    sqlite.execSQL("DELETE FROM stores WHERE isCustom = 1");
    settingsPreferences.setSortDirection(SortDirection.ASC);
    settingsPreferences.setLastUsedStoreId(null);
    settingsPreferences.setDefaultStoreId(null);
    for (StoreBrand brand : StoreLocatorPages.brandOrder()) {
      selectedStoreRepository.clear(brand);
    }
  }

  @Test
  public void createListAddEditCheckAndSortItems() {
    Intent intent =
        new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
      waitFor(
          () -> onFocusedView(withText(R.string.lists_empty_title)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.create_list_button)).perform(click());
      onFocusedView(withText(android.R.string.ok)).perform(click());

      waitFor(() -> onFocusedView(withId(R.id.list_detail_name)).check(matches(isDisplayed())));

      // Empty list shows no store sections until a store is added.
      waitForText(R.string.no_stores);
      onFocusedView(withId(R.id.add_store_button)).perform(click());
      androidx.test.espresso.Espresso.onView(withId(R.id.store_name_input))
          .inRoot(isDialog())
          .perform(replaceText("Corner Market"), pressImeActionButton());

      waitForContentDescription("Add item to Corner Market");
      onFocusedView(withContentDescription("Add item to Corner Market"))
          .perform(replaceText("milk"), clickAddItemButton());
      waitForItemText("milk");

      onFocusedView(withId(R.id.list_detail_recycler))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(itemTextMatcher("milk")),
                  clickChildViewWithId(R.id.item_overflow)));
      onFocusedView(withText(R.string.set_aisle)).perform(click());
      onFocusedView(withId(R.id.dialog_aisle_input)).perform(replaceText("A12"));
      onFocusedView(withText(R.string.save)).perform(click());
      waitForText("Aisle A12");

      clickItemCheckbox("milk");
      checkItemCheckbox("milk", true);

      onFocusedView(withId(R.id.sort_toggle_button)).perform(click());
      onFocusedView(withId(R.id.sort_toggle_button))
          .check(matches(withContentDescription(R.string.sort_descending)));

      onFocusedView(withId(R.id.list_detail_recycler))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(itemTextMatcher("milk")),
                  clickChildViewWithId(R.id.item_overflow)));
      onFocusedView(withText(R.string.edit_item)).perform(click());
      onFocusedView(withId(R.id.raw_text_input)).check(matches(withText("milk")));
      onFocusedView(withId(R.id.raw_text_input)).perform(replaceText("milk 2%"));
      onFocusedView(withId(R.id.save_item_button)).perform(click());
      waitForItemText("milk 2%");

      onFocusedView(withContentDescription("Add item to Corner Market"))
          .perform(replaceText("eggs"));
      waitForText("eggs");
      scenario.recreate();
      waitForContentDescription("Add item to Corner Market");
      onFocusedView(withText("eggs")).check(matches(isDisplayed()));
    }
  }

  @Test
  public void addStoreDialogListsSupportedStoresAndStillCreatesCustomStore() {
    Intent intent =
        new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
      int storesBefore = storeRepository.getActive().size();
      waitFor(
          () -> onFocusedView(withText(R.string.lists_empty_title)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.create_list_button)).perform(click());
      onFocusedView(withText(android.R.string.ok)).perform(click());
      waitFor(() -> onFocusedView(withId(R.id.list_detail_name)).check(matches(isDisplayed())));
      waitForText(R.string.no_stores);

      onFocusedView(withId(R.id.add_store_button)).perform(click());
      onView(withId(R.id.store_spinner)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withId(R.id.store_name_input)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withId(R.id.store_spinner)).inRoot(isDialog()).perform(click());
      onView(withText("Home Depot")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
      onView(withText("Walmart")).inRoot(isPlatformPopup()).perform(click());
      onView(withText(R.string.add_store_add)).inRoot(isDialog()).perform(click());

      waitForBranchPicker();
      onFocusedView(withId(R.id.branchBrandSpinner)).check(matches(withSpinnerValue("Walmart")));
      pressBack();
      waitForContentDescription("Add item to Walmart");
      assertEquals(
          "supported store must be reused", storesBefore, storeRepository.getActive().size());
      onFocusedView(withContentDescription("Add item to Walmart"))
          .perform(replaceText("milk"), clickAddItemButton());
      waitForItemText("milk");

      onFocusedView(withId(R.id.add_store_button)).perform(click());
      onView(withId(R.id.store_spinner)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withId(R.id.store_name_input))
          .inRoot(isDialog())
          .perform(replaceText("Corner Market"), pressImeActionButton());

      waitForContentDescription("Add item to Corner Market");
      onView(withText(R.string.branch_picker_title)).check(doesNotExist());
      assertEquals(
          "custom name must create one new store",
          storesBefore + 1,
          storeRepository.getActive().size());
    }
  }

  @Test
  public void addStoreOpensPrefilledBranchPickerForEverySupportedBrand() {
    Intent intent =
        new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
      waitFor(
          () -> onFocusedView(withText(R.string.lists_empty_title)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.create_list_button)).perform(click());
      onFocusedView(withText(android.R.string.ok)).perform(click());
      waitFor(() -> onFocusedView(withId(R.id.list_detail_name)).check(matches(isDisplayed())));

      for (StoreBrand brand : StoreLocatorPages.brandOrder()) {
        String label = brandLabel(brand);
        onFocusedView(withId(R.id.add_store_button)).perform(click());
        onView(withId(R.id.store_spinner)).inRoot(isDialog()).perform(click());
        onView(withText(label)).inRoot(isPlatformPopup()).perform(click());
        onView(withText(R.string.add_store_add)).inRoot(isDialog()).perform(click());

        waitForBranchPicker();
        onFocusedView(withId(R.id.branchBrandSpinner)).check(matches(withSpinnerValue(label)));
        pressBack();
        waitForContentDescription("Add item to " + label);
      }
    }
  }

  @Test
  public void addStoreWithSavedBranchDoesNotOpenBranchPicker() {
    selectedStoreRepository.select(branch(StoreBrand.WALMART));
    Intent intent =
        new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
      waitFor(
          () -> onFocusedView(withText(R.string.lists_empty_title)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.create_list_button)).perform(click());
      onFocusedView(withText(android.R.string.ok)).perform(click());
      waitFor(() -> onFocusedView(withId(R.id.list_detail_name)).check(matches(isDisplayed())));
      waitForText(R.string.no_stores);

      onFocusedView(withId(R.id.add_store_button)).perform(click());
      onView(withId(R.id.store_spinner)).inRoot(isDialog()).perform(click());
      onView(withText("Walmart")).inRoot(isPlatformPopup()).perform(click());
      onView(withText(R.string.add_store_add)).inRoot(isDialog()).perform(click());

      waitForContentDescription("Add item to Walmart");
      onView(withText(R.string.branch_picker_title)).check(doesNotExist());
    }
  }

  private static void scrollToPosition(int position) {
    onFocusedView(withId(R.id.list_detail_recycler))
        .perform(RecyclerViewActions.scrollToPosition(position));
  }

  private static void clickItemCheckbox(String itemText) {
    waitForItemText(itemText);
    onFocusedView(withId(R.id.list_detail_recycler))
        .perform(
            RecyclerViewActions.actionOnItem(
                hasDescendant(itemTextMatcher(itemText)),
                clickChildViewWithId(R.id.item_checkbox)));
  }

  private static void checkItemCheckbox(String itemText, boolean checked) {
    waitForItemText(itemText);
    onFocusedView(withId(R.id.list_detail_recycler))
        .perform(RecyclerViewActions.scrollTo(hasDescendant(itemTextMatcher(itemText))));
    onFocusedView(withId(R.id.list_detail_recycler))
        .check(
            matches(
                hasDescendant(
                    allOf(
                        hasDescendant(itemTextMatcher(itemText)),
                        hasDescendant(
                            allOf(
                                withId(R.id.item_checkbox),
                                checked
                                    ? isChecked()
                                    : org.hamcrest.Matchers.not(isChecked())))))));
  }

  private static ViewAction clickChildViewWithId(int viewId) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isDisplayed();
      }

      @Override
      public String getDescription() {
        return "click child view with id " + viewId;
      }

      @Override
      public void perform(UiController uiController, View view) {
        View child = view.findViewById(viewId);
        if (child == null) {
          throw new AssertionError("No child view with id " + viewId);
        }
        child.performClick();
        uiController.loopMainThreadUntilIdle();
      }
    };
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

  private static ViewAction clickAddItemButton() {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isAssignableFrom(EditText.class);
      }

      @Override
      public String getDescription() {
        return "click the add item button of the same row";
      }

      @Override
      public void perform(UiController uiController, View view) {
        View ancestor = view;
        while (ancestor.getParent() instanceof View) {
          ancestor = (View) ancestor.getParent();
          View button = ancestor.findViewById(R.id.add_item_button);
          if (button != null) {
            button.performClick();
            uiController.loopMainThreadUntilIdle();
            return;
          }
        }
        throw new AssertionError("add item row has no add_item_button");
      }
    };
  }

  private static void waitForText(String text) {
    waitFor(() -> onFocusedView(withText(text)).check(matches(isDisplayed())));
  }

  private static void waitForText(int textRes) {
    waitFor(() -> onFocusedView(withText(textRes)).check(matches(isDisplayed())));
  }

  /** Matches the raw text of a list item row, ignoring the hidden display name field. */
  private static Matcher<View> itemTextMatcher(String text) {
    return allOf(withId(R.id.item_raw_text), withText(text));
  }

  private static void waitForItemText(String text) {
    waitFor(() -> onFocusedView(itemTextMatcher(text)).check(matches(isDisplayed())));
  }

  private static void waitForContentDescription(String description) {
    waitFor(() -> onFocusedView(withContentDescription(description)).check(matches(isDisplayed())));
  }

  private static void waitForBranchPicker() {
    waitFor(
        () -> onFocusedView(withText(R.string.branch_picker_title)).check(matches(isDisplayed())));
  }

  private static Matcher<View> withSpinnerValue(String expected) {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(View view) {
        if (!(view instanceof Spinner spinner) || spinner.getSelectedItem() == null) {
          return false;
        }
        return expected.equals(spinner.getSelectedItem().toString());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("spinner with selection \"").appendText(expected).appendText("\"");
      }
    };
  }

  private static String brandLabel(StoreBrand brand) {
    int resId =
        switch (brand) {
          case WALMART -> R.string.brand_walmart;
          case HOME_DEPOT -> R.string.brand_home_depot;
          case LOWES -> R.string.brand_lowes;
          case TARGET -> R.string.brand_target;
          case SMITHS -> R.string.brand_smiths;
          case CUSTOM -> throw new IllegalArgumentException("custom has no locator brand");
        };
    return ApplicationProvider.getApplicationContext().getString(resId);
  }

  private static NearbyStore branch(StoreBrand brand) {
    return new NearbyStore(
        "branch-" + brand.name(),
        brand,
        brand.getDisplayName() + " #123",
        "1 Main St",
        "Denver",
        "CO",
        null,
        null,
        null,
        NearbyStoreSource.LOCATOR);
  }

  private static void waitFor(Runnable assertion) {
    TestWait.waitFor(assertion);
  }
}
