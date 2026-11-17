/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.Worker;
import androidx.work.testing.TestWorkerBuilder;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.testing.FakeClock;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.time.LocalDate;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class ScheduleCheckWorkerTest {

  private static final LocalDate DUE_DATE = LocalDate.of(2026, 1, 1);
  private static final LocalDate ADVANCED_DATE = LocalDate.of(2026, 1, 15);

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;
  @Inject ScheduleRepository scheduleRepository;
  @Inject ShoppingListRepository listRepository;

  private ExecutorService executor;
  private TimeZone originalTimeZone;

  @Before
  public void setUp() {
    originalTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    hiltRule.inject();
    clearData();
    executor = Executors.newSingleThreadExecutor();
  }

  @After
  public void tearDown() {
    clearData();
    executor.shutdownNow();
    TimeZone.setDefault(originalTimeZone);
  }

  @Test
  public void createsDueListAndAdvancesSchedule() {
    insertSchedule();

    Worker.Result result = runWorker();

    assertEquals(Worker.Result.success(), result);
    ShoppingList created = listRepository.findNearestActiveOnOrAfter(DUE_DATE);
    assertNotNull(created);
    assertEquals(DUE_DATE, created.getShoppingDate());
    assertTrue(created.isAutoCreated());
    assertEquals("Shopping list 2026-01-01", created.getName());
    assertEquals(ADVANCED_DATE, scheduleRepository.getById("schedule-1").getNextShoppingDate());
    assertEquals(DUE_DATE, scheduleRepository.getById("schedule-1").getLastCreatedDate());
  }

  @Test
  public void doesNotCreateDuplicateForDueDate() {
    listRepository.getOrCreate(DUE_DATE, "Existing list", null, false);
    insertSchedule();

    Worker.Result result = runWorker();

    assertEquals(Worker.Result.success(), result);
    assertEquals(1, listRepository.getActive().size());
    ShoppingList existing = listRepository.findNearestActiveOnOrAfter(DUE_DATE);
    assertNotNull(existing);
    assertEquals("Existing list", existing.getName());
    assertEquals(ADVANCED_DATE, scheduleRepository.getById("schedule-1").getNextShoppingDate());
  }

  private Worker.Result runWorker() {
    Context context = ApplicationProvider.getApplicationContext();
    ScheduleCheckWorker worker =
        TestWorkerBuilder.from(context, ScheduleCheckWorker.class, executor).build();
    return worker.doWork();
  }

  private void insertSchedule() {
    scheduleRepository.insert(
        new ShoppingSchedule(
            "schedule-1",
            2,
            IntervalUnit.WEEKS,
            DUE_DATE,
            true,
            false,
            null,
            FakeClock.NOW,
            FakeClock.NOW));
  }

  private void clearData() {
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM shopping_schedules");
  }
}
