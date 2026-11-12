/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.fastshoppr.app.domain.concurrent.MainThreadExecutor;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.Executor;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public final class MainThreadModule {

  private MainThreadModule() {}

  @Provides
  @Singleton
  static MainThreadExecutor provideMainThreadExecutor(@ApplicationContext Context context) {
    Executor mainExecutor = ContextCompat.getMainExecutor(context);
    return mainExecutor::execute;
  }
}
