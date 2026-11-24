/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.ui.lookup.AndroidStoreContextFormatter;
import com.fastshoppr.app.ui.lookup.StoreContextFormatter;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class UiTextModule {

  @Binds
  @Singleton
  abstract StoreContextFormatter bindStoreContextFormatter(
      AndroidStoreContextFormatter implementation);
}
