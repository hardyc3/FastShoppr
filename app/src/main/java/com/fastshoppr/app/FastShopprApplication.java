/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.work.Configuration;
import com.fastshoppr.app.work.ScheduleWorkerDeps;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public final class FastShopprApplication extends Application implements Configuration.Provider {

  @Override
  public void onCreate() {
    super.onCreate();
    try {
      ScheduleWorkerDeps.get(this).scheduleWorkerScheduler().ensurePeriodicCheck();
    } catch (Exception ignored) {
      // best effort
    }
  }

  @NonNull
  @Override
  public Configuration getWorkManagerConfiguration() {
    return new Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build();
  }
}
