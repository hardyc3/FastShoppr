/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.time.Clock;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class OrganizingScheduler {

  private final Context context;
  private final ListItemDao listItemDao;
  private final Clock clock;
  private final ExecutorService executor;
  private final NotificationHelper notificationHelper;
  private final OrganizingProgressStore progressStore;

  @Inject
  public OrganizingScheduler(
      @ApplicationContext Context context,
      ListItemDao listItemDao,
      Clock clock,
      ExecutorService executor,
      NotificationHelper notificationHelper,
      OrganizingProgressStore progressStore) {
    this.context = context;
    this.listItemDao = listItemDao;
    this.clock = clock;
    this.executor = executor;
    this.notificationHelper = notificationHelper;
    this.progressStore = progressStore;
  }

  public void scheduleAfterSave(String listId) {
    enqueue(listId, false, ExistingWorkPolicy.KEEP);
  }

  public void organizeNow(String listId) {
    enqueue(listId, false, ExistingWorkPolicy.REPLACE);
  }

  public void retryFailed(String listId) {
    executor.execute(
        () -> {
          long now = clock.nowMillis();
          listItemDao.setLookupStatusForList(
              listId,
              List.of(
                  LookupStatus.ERROR.name(),
                  LookupStatus.NOT_FOUND.name(),
                  LookupStatus.NEEDS_REVIEW.name()),
              LookupStatus.QUEUED.name(),
              now);
          enqueue(listId, false, ExistingWorkPolicy.REPLACE);
        });
  }

  private void enqueue(String listId, boolean forceRefresh, ExistingWorkPolicy policy) {
    if (listId == null) {
      return;
    }
    executor.execute(
        () -> {
          long now = clock.nowMillis();
          // reset interrupted in-progress items so process death doesn't lose them
          listItemDao.setLookupStatusForList(
              listId, List.of(LookupStatus.IN_PROGRESS.name()), LookupStatus.QUEUED.name(), now);
          listItemDao.setLookupStatusForList(
              listId, List.of(LookupStatus.PENDING.name()), LookupStatus.QUEUED.name(), now);

          Constraints constraints =
              new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
          Data data =
              new Data.Builder()
                  .putString(OrganizingContracts.KEY_LIST_ID, listId)
                  .putBoolean(OrganizingContracts.KEY_FORCE_REFRESH, forceRefresh)
                  .build();
          OneTimeWorkRequest request =
              new OneTimeWorkRequest.Builder(OrganizeListWorker.class)
                  .setInputData(data)
                  .setConstraints(constraints)
                  .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                  .addTag(OrganizingContracts.organizeWorkName(listId))
                  .build();
          WorkManager.getInstance(context)
              .enqueueUniqueWork(OrganizingContracts.organizeWorkName(listId), policy, request);
        });
  }

  public void cancel(String listId) {
    if (listId == null) {
      return;
    }
    WorkManager.getInstance(context).cancelUniqueWork(OrganizingContracts.organizeWorkName(listId));
    WorkManager.getInstance(context)
        .cancelUniqueWork(OrganizingContracts.homeDepotWorkName(listId));
    // stop foreground service if running for this list
    Intent stop = new Intent(context, WalmartLookupForegroundService.class);
    stop.setAction(OrganizingContracts.ACTION_CANCEL);
    stop.putExtra(OrganizingContracts.EXTRA_LIST_ID, listId);
    context.stopService(stop);
    notificationHelper.cancel(listId);
    progressStore.clear(listId);
    // Keep completed results; return remaining in-flight items to pending for a later run.
    executor.execute(
        () -> {
          try {
            long now = clock.nowMillis();
            listItemDao.setLookupStatusForList(
                listId,
                List.of(LookupStatus.IN_PROGRESS.name(), LookupStatus.QUEUED.name()),
                LookupStatus.PENDING.name(),
                now);
          } catch (Exception ignored) {
          }
        });
  }

  public boolean canPostNotifications() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return true;
    }
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED;
  }
}
