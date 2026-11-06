/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import android.content.Context;
import androidx.room.Room;
import com.fastshoppr.app.data.local.FastShopprDatabase;
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
  static FastShopprDatabase provideDatabase(@ApplicationContext Context context) {
    return Room.databaseBuilder(context, FastShopprDatabase.class, FastShopprDatabase.NAME).build();
  }
}
