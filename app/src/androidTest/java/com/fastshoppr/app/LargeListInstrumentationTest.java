/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Bundle;
import android.os.SystemClock;
import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.dao.StoreDao;
import com.fastshoppr.app.data.local.entity.ListItemEntity;
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class LargeListInstrumentationTest {

  private static final String LIST_ID = "large-list";
  private static final String LIST_DATE = "2026-11-20";
  private static final int ITEM_COUNT = 120;

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;
  @Inject ShoppingListDao listDao;
  @Inject ListItemDao itemDao;
  @Inject StoreDao storeDao;

  @Before
  public void setUp() {
    hiltRule.inject();
    clearUserData();
    assertTrue("built-in stores must be seeded", !storeDao.getActive().isEmpty());

    listDao.insert(
        new ShoppingListEntity(LIST_ID, "Large list", LIST_DATE, null, false, false, null, 1L, 1L));
    String storeId = storeDao.getActive().get(0).id;
    for (int i = 0; i < ITEM_COUNT; i++) {
      itemDao.insert(
          new ListItemEntity(
              "large-item-" + i,
              LIST_ID,
              storeId,
              "Item " + i,
              "Item " + i,
              "item " + i,
              null,
              LIST_DATE,
              "A" + (i % 12),
              null,
              null,
              null,
              null,
              LookupStatus.FOUND,
              null,
              null,
              null,
              ItemSource.TYPED,
              false,
              i,
              null,
              1L,
              1L));
    }
  }

  @After
  public void tearDown() {
    clearUserData();
  }

  private void clearUserData() {
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM aisle_cache");
    sqlite.execSQL("DELETE FROM shopping_schedules");
  }

  @Test
  public void rendersAndSortsOneHundredTwentyItemsWithoutUiStall() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.list_detail, NavArguments.forList(LIST_ID));
      waitForRows(scenario);
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();

      AtomicReference<CharSequence> initialDescription = new AtomicReference<>();
      scenario.onActivity(
          activity ->
              initialDescription.set(
                  activity.findViewById(R.id.sort_toggle_button).getContentDescription()));
      assertTrue("sort toggle must have a description", initialDescription.get() != null);

      long startedAt = SystemClock.elapsedRealtime();
      scenario.onActivity(
          activity -> activity.findViewById(R.id.sort_toggle_button).performClick());
      CharSequence updatedDescription = null;
      while (SystemClock.elapsedRealtime() - startedAt < 2_000L) {
        AtomicReference<CharSequence> currentDescription = new AtomicReference<>();
        scenario.onActivity(
            activity ->
                currentDescription.set(
                    activity.findViewById(R.id.sort_toggle_button).getContentDescription()));
        if (currentDescription.get() != null
            && !currentDescription.get().equals(initialDescription.get())) {
          updatedDescription = currentDescription.get();
          break;
        }
        SystemClock.sleep(10L);
      }
      long elapsed = SystemClock.elapsedRealtime() - startedAt;

      assertTrue("sort toggle did not change direction", updatedDescription != null);
      assertTrue("sort toggle took " + elapsed + " ms", elapsed < 1_000L);
      waitForRows(scenario);
      scenario.onActivity(
          activity -> {
            RecyclerView view = activity.findViewById(R.id.list_detail_recycler);
            view.scrollToPosition(ITEM_COUNT);
          });
    }
  }

  private static void navigate(
      ActivityScenario<MainActivity> scenario, int destinationId, Bundle args) {
    scenario.onActivity(
        activity -> {
          androidx.fragment.app.Fragment host =
              activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
          assertTrue(host instanceof androidx.navigation.fragment.NavHostFragment);
          androidx.navigation.NavController navController =
              ((androidx.navigation.fragment.NavHostFragment) host).getNavController();
          navController.navigate(destinationId, args);
        });
  }

  private static void waitForRows(ActivityScenario<MainActivity> scenario) {
    long deadline = SystemClock.elapsedRealtime() + 10_000L;
    while (SystemClock.elapsedRealtime() < deadline) {
      if (adapterItemCount(scenario) >= ITEM_COUNT) {
        return;
      }
      SystemClock.sleep(50L);
    }
    fail("list detail did not render " + ITEM_COUNT + " rows");
  }

  private static int adapterItemCount(ActivityScenario<MainActivity> scenario) {
    AtomicInteger count = new AtomicInteger(-1);
    scenario.onActivity(
        activity -> {
          RecyclerView recycler = activity.findViewById(R.id.list_detail_recycler);
          count.set(
              recycler == null || recycler.getAdapter() == null
                  ? -1
                  : recycler.getAdapter().getItemCount());
        });
    return count.get();
  }
}
