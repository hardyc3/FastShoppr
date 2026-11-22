/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.data.local.SeedStores;
import com.fastshoppr.app.data.local.dao.UserDataDao;
import com.fastshoppr.app.data.local.entity.StoreEntity;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.UserDataRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.work.NotificationHelper;
import com.fastshoppr.app.work.OrganizingContracts;
import com.fastshoppr.app.work.OrganizingProgressStore;
import com.fastshoppr.app.work.ScheduleCheckWorker;
import com.fastshoppr.app.work.ScheduleContracts;
import com.fastshoppr.app.work.WalmartLookupForegroundService;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class UserDataRepositoryImpl implements UserDataRepository {

  private final Context context;
  private final UserDataDao userDataDao;
  private final SettingsRepository settingsRepository;
  private final CaptureImageStore captureImageStore;
  private final NotificationHelper notificationHelper;
  private final OrganizingProgressStore progressStore;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public UserDataRepositoryImpl(
      @ApplicationContext Context context,
      UserDataDao userDataDao,
      SettingsRepository settingsRepository,
      CaptureImageStore captureImageStore,
      NotificationHelper notificationHelper,
      OrganizingProgressStore progressStore,
      Clock clock,
      ExecutorService executor) {
    this.context = context;
    this.userDataDao = userDataDao;
    this.settingsRepository = settingsRepository;
    this.captureImageStore = captureImageStore;
    this.notificationHelper = notificationHelper;
    this.progressStore = progressStore;
    this.clock = clock;
    this.executor = executor;
  }

  @Override
  public CompletableFuture<Void> deleteAllData() {
    CompletableFuture<Void> deleted = new CompletableFuture<>();
    executor.execute(
        () -> {
          try {
            try {
              WorkManager.getInstance(context).cancelAllWork();
              // cancelAllWork also removes the daily schedule check; re-arm it so schedules
              // created later in this process are still picked up in the background.
              PeriodicWorkRequest scheduleCheck =
                  new PeriodicWorkRequest.Builder(ScheduleCheckWorker.class, 1, TimeUnit.DAYS)
                      .build();
              WorkManager.getInstance(context)
                  .enqueueUniquePeriodicWork(
                      ScheduleContracts.UNIQUE_WORK_SCHEDULE_CHECK,
                      ExistingPeriodicWorkPolicy.KEEP,
                      scheduleCheck);
            } catch (IllegalStateException notInitialized) {
              // WorkManager is initialized by FastShopprApplication in the app process.
              // Instrumented repository tests can run under a bare test Application, where there
              // is no work to cancel; data clearing must still succeed.
            }
            stopOrganizing();
            userDataDao.clearUserDataAndReseedStores(seedStores(clock.nowMillis()));
            settingsRepository.clearAll();
            captureImageStore.deleteAll();
            await(clearWebViewData());
            deleted.complete(null);
          } catch (Throwable error) {
            deleted.completeExceptionally(error);
          }
        });
    return deleted;
  }

  private void stopOrganizing() {
    // Set the shared flag directly so suppression holds even if the service intent cannot be
    // delivered (e.g. Android blocks a foreground-service start). stopService() only resolves the
    // component and never reaches onStartCommand, so startService() is required to carry the
    // action; the fallback still stops the service via onDestroy.
    progressStore.setSuppressCompletion(true);
    Intent stop = new Intent(context, WalmartLookupForegroundService.class);
    stop.setAction(OrganizingContracts.ACTION_CANCEL_ALL);
    try {
      context.startService(stop);
    } catch (IllegalStateException backgroundStartRestricted) {
      context.stopService(stop);
    }
    notificationHelper.cancelAll();
    progressStore.clearAll();
  }

  private CompletableFuture<Void> clearWebViewData() {
    CompletableFuture<Void> cleared = new CompletableFuture<>();
    if (Looper.myLooper() == Looper.getMainLooper()) {
      clearWebViewDataOnMain(cleared);
    } else {
      new Handler(Looper.getMainLooper()).post(() -> clearWebViewDataOnMain(cleared));
    }
    return cleared;
  }

  private static void clearWebViewDataOnMain(CompletableFuture<Void> cleared) {
    try {
      WebStorage.getInstance().deleteAllData();
      CookieManager.getInstance().removeAllCookies(ignored -> cleared.complete(null));
    } catch (RuntimeException error) {
      cleared.completeExceptionally(error);
    }
  }

  private static void await(CompletableFuture<Void> future) {
    try {
      future.get();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while clearing WebView data", error);
    } catch (ExecutionException error) {
      Throwable cause = error.getCause();
      if (cause instanceof RuntimeException runtimeError) {
        throw runtimeError;
      }
      throw new IllegalStateException("Could not clear WebView data", cause);
    }
  }

  private static List<StoreEntity> seedStores(long now) {
    List<StoreEntity> stores = new ArrayList<>();
    for (StoreBrand brand : SeedStores.brands()) {
      stores.add(
          new StoreEntity(
              SeedStores.idFor(brand),
              brand,
              SeedStores.nameFor(brand),
              null,
              null,
              null,
              null,
              null,
              null,
              false,
              false,
              false,
              null,
              now,
              now));
    }
    return stores;
  }
}
