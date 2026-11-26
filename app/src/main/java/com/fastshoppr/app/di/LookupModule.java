/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.data.remote.lookup.LookupConfig;
import com.fastshoppr.app.data.remote.lookup.RateLimiter;
import com.fastshoppr.app.data.remote.lookup.RetailHttpLookupService;
import com.fastshoppr.app.data.remote.lookup.RetailSiteConfig;
import com.fastshoppr.app.data.remote.walmart.WalmartWebViewLookupService;
import com.fastshoppr.app.domain.lookup.StoreLookupService;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;
import java.util.concurrent.ExecutorService;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;

@Module
@InstallIn(SingletonComponent.class)
public abstract class LookupModule {

  @Binds
  @IntoSet
  abstract StoreLookupService bindWalmartLookupService(WalmartWebViewLookupService service);

  @Provides
  @Singleton
  @IntoSet
  static StoreLookupService provideHomeDepotLookupService(
      OkHttpClient client, ExecutorService executor, RateLimiter rateLimiter, LookupConfig config) {
    return new RetailHttpLookupService(
        client, executor, rateLimiter, config, RetailSiteConfig.HOME_DEPOT);
  }

  @Provides
  @Singleton
  @IntoSet
  static StoreLookupService provideLowesLookupService(
      OkHttpClient client, ExecutorService executor, RateLimiter rateLimiter, LookupConfig config) {
    return new RetailHttpLookupService(
        client, executor, rateLimiter, config, RetailSiteConfig.LOWES);
  }

  @Provides
  @Singleton
  @IntoSet
  static StoreLookupService provideSmithsLookupService(
      OkHttpClient client, ExecutorService executor, RateLimiter rateLimiter, LookupConfig config) {
    return new RetailHttpLookupService(
        client, executor, rateLimiter, config, RetailSiteConfig.SMITHS);
  }
}
