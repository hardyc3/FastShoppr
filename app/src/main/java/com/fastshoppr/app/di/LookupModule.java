/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.data.remote.homedepot.HomeDepotHttpLookupService;
import com.fastshoppr.app.data.remote.walmart.WalmartWebViewLookupService;
import com.fastshoppr.app.domain.lookup.StoreLookupService;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;

@Module
@InstallIn(SingletonComponent.class)
public abstract class LookupModule {

  @Binds
  @IntoSet
  abstract StoreLookupService bindWalmartLookupService(WalmartWebViewLookupService service);

  @Binds
  @IntoSet
  abstract StoreLookupService bindHomeDepotLookupService(HomeDepotHttpLookupService service);
}
