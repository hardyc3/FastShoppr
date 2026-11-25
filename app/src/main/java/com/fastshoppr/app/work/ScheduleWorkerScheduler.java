/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.content.Context;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.fastshoppr.app.domain.usecase.CleanUpExpiredDataUseCase;
import com.fastshoppr.app.domain.usecase.CreateScheduledListUseCase;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ScheduleWorkerScheduler {

  private final Context context;
  private final CreateScheduledListUseCase createScheduledListUseCase;
  private final CleanUpExpiredDataUseCase cleanUpExpiredDataUseCase;
  private final ExecutorService executor;

  @Inject
  public ScheduleWorkerScheduler(
      @ApplicationContext Context context,
      CreateScheduledListUseCase createScheduledListUseCase,
      CleanUpExpiredDataUseCase cleanUpExpiredDataUseCase,
      ExecutorService executor) {
    this.context = context;
    this.createScheduledListUseCase = createScheduledListUseCase;
    this.cleanUpExpiredDataUseCase = cleanUpExpiredDataUseCase;
    this.executor = executor;
  }

  public void ensurePeriodicCheck() {
    PeriodicWorkRequest request =
        new PeriodicWorkRequest.Builder(ScheduleCheckWorker.class, 1, TimeUnit.DAYS).build();
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            ScheduleContracts.UNIQUE_WORK_SCHEDULE_CHECK, ExistingPeriodicWorkPolicy.KEEP, request);
    PeriodicWorkRequest maintenance =
        new PeriodicWorkRequest.Builder(MaintenanceWorker.class, 1, TimeUnit.DAYS).build();
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            ScheduleContracts.UNIQUE_WORK_CACHE_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            maintenance);
  }

  public void checkOnAppOpen() {
    // Run inline on background executor so behavior is correct even if periodic work delayed.
    executor.execute(
        () -> {
          try {
            cleanUpExpiredDataUseCase.run().join();
          } catch (Exception ignored) {
            // Best effort; the daily workers retry.
          }
          try {
            createScheduledListUseCase.runIfDue().join();
          } catch (Exception ignored) {
          }
        });
  }

  public void enqueueImmediateCheck() {
    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ScheduleCheckWorker.class).build();
    WorkManager.getInstance(context)
        .enqueueUniqueWork("schedule_check_once", ExistingWorkPolicy.REPLACE, request);
  }
}
