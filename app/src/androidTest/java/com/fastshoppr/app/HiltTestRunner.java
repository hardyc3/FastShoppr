/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.test.runner.AndroidJUnitRunner;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import dagger.hilt.android.testing.HiltTestApplication;

public final class HiltTestRunner extends AndroidJUnitRunner {

  private static boolean workManagerInitialized;

  @Override
  public Application newApplication(ClassLoader classLoader, String className, Context context)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    return super.newApplication(classLoader, HiltTestApplication.class.getName(), context);
  }

  @Override
  public void callApplicationOnCreate(Application app) {
    super.callApplicationOnCreate(app);
    if (!workManagerInitialized) {
      WorkManager.initialize(
          app, new Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build());
      workManagerInitialized = true;
    }
  }
}
