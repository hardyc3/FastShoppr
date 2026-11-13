/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.data.repository.AisleCacheRepositoryImpl;
import com.fastshoppr.app.data.repository.AisleLookupRepositoryImpl;
import com.fastshoppr.app.data.repository.CaptureRepositoryImpl;
import com.fastshoppr.app.data.repository.ListItemRepositoryImpl;
import com.fastshoppr.app.data.repository.ScheduleRepositoryImpl;
import com.fastshoppr.app.data.repository.SettingsRepositoryImpl;
import com.fastshoppr.app.data.repository.ShoppingListRepositoryImpl;
import com.fastshoppr.app.data.repository.StoreRepositoryImpl;
import com.fastshoppr.app.domain.repository.AisleCacheRepository;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

  @Binds
  @Singleton
  abstract ShoppingListRepository bindShoppingListRepository(
      ShoppingListRepositoryImpl implementation);

  @Binds
  @Singleton
  abstract ListItemRepository bindListItemRepository(ListItemRepositoryImpl implementation);

  @Binds
  @Singleton
  abstract StoreRepository bindStoreRepository(StoreRepositoryImpl implementation);

  @Binds
  @Singleton
  abstract ScheduleRepository bindScheduleRepository(ScheduleRepositoryImpl implementation);

  @Binds
  @Singleton
  abstract CaptureRepository bindCaptureRepository(CaptureRepositoryImpl implementation);

  @Binds
  @Singleton
  abstract AisleCacheRepository bindAisleCacheRepository(AisleCacheRepositoryImpl implementation);

  @Binds
  @Singleton
  abstract AisleLookupRepository bindAisleLookupRepository(
      AisleLookupRepositoryImpl implementation);

  @Binds
  @Singleton
  abstract SettingsRepository bindSettingsRepository(SettingsRepositoryImpl implementation);
}
