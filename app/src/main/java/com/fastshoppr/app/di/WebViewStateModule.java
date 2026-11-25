/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.data.repository.WebViewStateClearerImpl;
import com.fastshoppr.app.domain.port.WebViewStateClearer;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class WebViewStateModule {

  @Binds
  @Singleton
  abstract WebViewStateClearer bindWebViewStateClearer(WebViewStateClearerImpl implementation);
}
