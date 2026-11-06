/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import android.app.Application;
import android.content.Context;
import androidx.test.runner.AndroidJUnitRunner;
import dagger.hilt.android.testing.HiltTestApplication;

public final class HiltTestRunner extends AndroidJUnitRunner {

  @Override
  public Application newApplication(ClassLoader classLoader, String className, Context context)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    return super.newApplication(classLoader, HiltTestApplication.class.getName(), context);
  }
}
