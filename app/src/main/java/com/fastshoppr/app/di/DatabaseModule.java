/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import android.content.Context;
import androidx.room.Room;
import com.fastshoppr.app.data.local.DatabaseSeedCallback;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.Migrations;
import com.fastshoppr.app.data.local.dao.AisleCacheDao;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.dao.ShoppingScheduleDao;
import com.fastshoppr.app.data.local.dao.StoreDao;
import com.fastshoppr.app.data.local.dao.UserDataDao;
import com.fastshoppr.app.domain.time.Clock;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public final class DatabaseModule {

  private DatabaseModule() {}

  @Provides
  @Singleton
  static FastShopprDatabase provideDatabase(@ApplicationContext Context context, Clock clock) {
    return Room.databaseBuilder(context, FastShopprDatabase.class, FastShopprDatabase.NAME)
        .addMigrations(
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
            Migrations.MIGRATION_5_6)
        .addCallback(new DatabaseSeedCallback(clock))
        .build();
  }

  @Provides
  static StoreDao provideStoreDao(FastShopprDatabase database) {
    return database.storeDao();
  }

  @Provides
  static ShoppingListDao provideShoppingListDao(FastShopprDatabase database) {
    return database.shoppingListDao();
  }

  @Provides
  static ListItemDao provideListItemDao(FastShopprDatabase database) {
    return database.listItemDao();
  }

  @Provides
  static CaptureSessionDao provideCaptureSessionDao(FastShopprDatabase database) {
    return database.captureSessionDao();
  }

  @Provides
  static ShoppingScheduleDao provideShoppingScheduleDao(FastShopprDatabase database) {
    return database.shoppingScheduleDao();
  }

  @Provides
  static AisleCacheDao provideAisleCacheDao(FastShopprDatabase database) {
    return database.aisleCacheDao();
  }

  @Provides
  static UserDataDao provideUserDataDao(FastShopprDatabase database) {
    return database.userDataDao();
  }
}
