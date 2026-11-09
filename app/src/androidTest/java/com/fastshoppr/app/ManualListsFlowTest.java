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
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.EditText;
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
import com.fastshoppr.app.domain.model.SortDirection;
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

  @Before
  public void setUp() {
    hiltRule.inject();
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM aisle_cache");
    settingsPreferences.setSortDirection(SortDirection.ASC);
    settingsPreferences.setLastUsedStoreId(null);
    settingsPreferences.setDefaultStoreId(null);
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

      waitForText("Shopping list");
      onFocusedView(withId(R.id.store_selector_button)).perform(click());
      onFocusedView(withText("Home Depot")).perform(click());
      onFocusedView(withText(android.R.string.ok)).perform(click());
      waitForText("Store: Home Depot");
      waitForContentDescription("Add item to Walmart");
      onFocusedView(withContentDescription("Add item to Walmart"))
          .perform(replaceText("milk"), clickAddItemButton());
      waitForText("milk");

      scrollToPosition(2);
      onFocusedView(withId(R.id.item_overflow)).perform(click());
      onFocusedView(withText(R.string.set_aisle)).perform(click());
      onFocusedView(withId(R.id.dialog_aisle_input)).perform(replaceText("A12"));
      onFocusedView(withText(R.string.save)).perform(click());
      waitForText("Aisle A12");

      scrollToPosition(1);
      onFocusedView(withId(R.id.item_checkbox)).perform(click());
      onFocusedView(withId(R.id.item_checkbox)).check(matches(isChecked()));

      onFocusedView(withId(R.id.sort_toggle_button)).perform(click());
      onFocusedView(withId(R.id.sort_toggle_button))
          .check(matches(withText(R.string.sort_descending)));

      scrollToPosition(1);
      onFocusedView(withId(R.id.item_overflow)).perform(click());
      onFocusedView(withText(R.string.edit_item)).perform(click());
      onFocusedView(withId(R.id.raw_text_input)).check(matches(withText("milk")));
      onFocusedView(withId(R.id.raw_text_input)).perform(replaceText("milk 2%"));
      onFocusedView(withId(R.id.save_item_button)).perform(click());
      waitForText("milk 2%");

      scrollToPosition(2);
      onFocusedView(withContentDescription("Add item to Walmart")).perform(replaceText("eggs"));
      waitForText("eggs");
      scenario.recreate();
      waitForContentDescription("Add item to Walmart");
      scrollToPosition(2);
      onFocusedView(withText("eggs")).check(matches(isDisplayed()));
    }
  }

  private static void scrollToPosition(int position) {
    onFocusedView(withId(R.id.list_detail_recycler))
        .perform(RecyclerViewActions.scrollToPosition(position));
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

  private static void waitForContentDescription(String description) {
    waitFor(() -> onFocusedView(withContentDescription(description)).check(matches(isDisplayed())));
  }

  private static void waitFor(Runnable assertion) {
    long deadline = SystemClock.uptimeMillis() + 5_000L;
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
