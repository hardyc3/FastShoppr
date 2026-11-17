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
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.os.SystemClock;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.preferences.SettingsPreferences;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.time.LocalDate;
import java.util.TimeZone;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class CalendarFlowTest {

  private static final LocalDate LIST_DATE = LocalDate.of(2026, 1, 15);
  private static final LocalDate EMPTY_DATE = LocalDate.of(2026, 1, 20);

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;
  @Inject ShoppingListRepository listRepository;
  @Inject SettingsPreferences settingsPreferences;

  private TimeZone originalTimeZone;

  @Before
  public void setUp() {
    originalTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    hiltRule.inject();
    clearData();
    settingsPreferences.setLastCalendarMonth("2026-01");
    listRepository.getOrCreate(LIST_DATE, "January list", null, false);
  }

  @After
  public void tearDown() {
    clearData();
    TimeZone.setDefault(originalTimeZone);
  }

  @Test
  public void showsBadgesOpensListsCreatesListsAndChangesMonths() {
    Intent intent =
        new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
      waitFor(() -> onView(withId(R.id.open_calendar_button)).check(matches(isDisplayed())));
      onView(withId(R.id.open_calendar_button)).perform(click());

      waitFor(() -> onView(withId(R.id.textMonth)).check(matches(withText("January 2026"))));
      onView(withContentDescription("2026-01-15, has shopping list")).check(matches(isDisplayed()));
      onView(withText("January list · 2026-01-15")).check(matches(isDisplayed()));

      onView(withContentDescription("2026-01-15, has shopping list")).perform(click());
      waitFor(() -> onView(withId(R.id.list_detail_name)).check(matches(withText("January list"))));
      pressBack();

      waitFor(() -> onView(withId(R.id.textMonth)).check(matches(withText("January 2026"))));
      onView(withContentDescription("2026-01-20, no list")).perform(click());
      String createPrompt =
          ApplicationProvider.getApplicationContext()
              .getString(R.string.calendar_create_list_prompt, EMPTY_DATE.toString());
      onView(withText(createPrompt)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withText(R.string.create_list)).inRoot(isDialog()).perform(click());
      waitFor(
          () ->
              onView(withContentDescription("2026-01-20, has shopping list"))
                  .check(matches(isDisplayed())));

      onView(withId(R.id.btnNextMonth)).perform(click());
      waitFor(() -> onView(withId(R.id.textMonth)).check(matches(withText("February 2026"))));
    }
  }

  private void clearData() {
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM shopping_schedules");
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
