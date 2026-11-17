/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.testing.FakeClock;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class OverdueDialogTest {

  private static final LocalDate PAST_DATE = LocalDate.of(2025, 12, 30);
  private static final LocalDate NEXT_DATE = LocalDate.of(2026, 1, 5);

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;
  @Inject ShoppingListRepository listRepository;
  @Inject ListItemRepository itemRepository;
  @Inject StoreRepository storeRepository;

  private TimeZone originalTimeZone;
  private ShoppingList past;
  private ShoppingList next;
  private String uncheckedId;

  @Before
  public void setUp() {
    originalTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    hiltRule.inject();
    clearData();
    past = listRepository.getOrCreate(PAST_DATE, "Past list", null, false);
    next = listRepository.getOrCreate(NEXT_DATE, "Next list", null, false);
    Store store = storeRepository.findByBrand(StoreBrand.WALMART);

    ListItem unchecked = item(past.getId(), store.getId(), "unchecked-item", false, 0);
    uncheckedId = unchecked.getId();
    itemRepository.insert(unchecked);
    itemRepository.insert(item(past.getId(), store.getId(), "checked-item", true, 1));
  }

  @After
  public void tearDown() {
    clearData();
    TimeZone.setDefault(originalTimeZone);
  }

  @Test
  public void dialogOffersAllOptionsAndMovesOnlyUncheckedItems() {
    try (ActivityScenario<MainActivity> ignored = launchAndWaitForDialog()) {
      String move = moveButtonText();

      onView(withText(move)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withText(R.string.overdue_keep)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withText(R.string.overdue_missed)).inRoot(isDialog()).check(matches(isDisplayed()));

      onView(withText(move)).inRoot(isDialog()).perform(click());

      waitUntil(
          () -> itemRepository.getByList(next.getId()).size() == 1,
          "unchecked item did not move to the next list");
      List<ListItem> remaining = itemRepository.getByList(past.getId());
      assertEquals(1, remaining.size());
      assertTrue(remaining.get(0).isChecked());
      ListItem moved = itemRepository.getByList(next.getId()).get(0);
      assertEquals(uncheckedId, moved.getId());
      assertEquals(NEXT_DATE, moved.getTargetDate());
    }
  }

  @Test
  public void keepLeavesItemsOnPastList() {
    try (ActivityScenario<MainActivity> ignored = launchAndWaitForDialog()) {
      onView(withText(R.string.overdue_keep)).inRoot(isDialog()).perform(click());

      waitUntil(
          () -> {
            try {
              onView(withText(R.string.overdue_title)).check(doesNotExist());
              return true;
            } catch (AssertionError | RuntimeException exception) {
              return false;
            }
          },
          "overdue dialog did not dismiss");
      assertEquals(2, itemRepository.getByList(past.getId()).size());
      assertTrue(itemRepository.getByList(next.getId()).isEmpty());
      assertFalse(listRepository.getById(past.getId()).isArchived());
    }
  }

  @Test
  public void missedArchivesPastListWithoutMovingItems() {
    try (ActivityScenario<MainActivity> ignored = launchAndWaitForDialog()) {
      onView(withText(R.string.overdue_missed)).inRoot(isDialog()).perform(click());

      waitUntil(
          () -> listRepository.getById(past.getId()).isArchived(), "past list was not archived");
      assertTrue(itemRepository.getByList(next.getId()).isEmpty());
      assertTrue(
          listRepository.getActive().stream().noneMatch(list -> list.getId().equals(past.getId())));
    }
  }

  private ActivityScenario<MainActivity> launchAndWaitForDialog() {
    Intent intent =
        new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);
    try {
      waitUntil(
          () -> {
            try {
              onView(withText(R.string.overdue_title))
                  .inRoot(isDialog())
                  .check(matches(isDisplayed()));
              return true;
            } catch (AssertionError | RuntimeException exception) {
              return false;
            }
          },
          "overdue dialog did not appear");
      return scenario;
    } catch (Throwable error) {
      scenario.close();
      throw error;
    }
  }

  private String moveButtonText() {
    return ApplicationProvider.getApplicationContext().getString(R.string.overdue_move)
        + " ("
        + NEXT_DATE
        + ")";
  }

  private ListItem item(String listId, String storeId, String id, boolean checked, int position) {
    return ListItem.builder()
        .id(id)
        .listId(listId)
        .storeId(storeId)
        .rawText(id)
        .displayName(id)
        .normalizedText(id)
        .targetDate(PAST_DATE)
        .isChecked(checked)
        .sortPosition(position)
        .createdAt(FakeClock.NOW)
        .updatedAt(FakeClock.NOW)
        .build();
  }

  private void clearData() {
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM shopping_schedules");
  }

  private static void waitUntil(BooleanSupplier condition, String message) {
    long deadline = SystemClock.uptimeMillis() + 5_000L;
    while (SystemClock.uptimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      SystemClock.sleep(50L);
    }
    assertTrue(message, condition.getAsBoolean());
  }
}
