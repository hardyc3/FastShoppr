/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public final class ExecutorsModule {

  private ExecutorsModule() {}

  @Provides
  @Singleton
  static ExecutorService provideExecutorService() {
    return Executors.newFixedThreadPool(4);
  }
}
