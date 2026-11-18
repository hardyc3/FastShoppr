/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;
import androidx.annotation.NonNull;
import androidx.work.Configuration;
import com.fastshoppr.app.work.ScheduleWorkerDeps;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public final class FastShopprApplication extends Application implements Configuration.Provider {

  @Override
  public void onCreate() {
    super.onCreate();
    if (isDebuggable()) {
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder()
              .detectDiskReads()
              .detectDiskWrites()
              .detectNetwork()
              .penaltyLog()
              .build());
      StrictMode.setVmPolicy(
          new StrictMode.VmPolicy.Builder()
              .detectLeakedClosableObjects()
              .detectLeakedRegistrationObjects()
              .penaltyLog()
              .build());
    }
    try {
      ScheduleWorkerDeps.get(this).scheduleWorkerScheduler().ensurePeriodicCheck();
    } catch (Exception ignored) {
      // best effort
    }
  }

  @NonNull
  @Override
  public Configuration getWorkManagerConfiguration() {
    int minimumLoggingLevel = isDebuggable() ? android.util.Log.INFO : android.util.Log.WARN;
    return new Configuration.Builder().setMinimumLoggingLevel(minimumLoggingLevel).build();
  }

  private boolean isDebuggable() {
    return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
  }
}
