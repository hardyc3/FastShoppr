/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import android.content.Context;
import android.content.SharedPreferences;
import com.fastshoppr.app.data.preferences.SettingsPreferences;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public final class PreferencesModule {

  private PreferencesModule() {}

  @Provides
  @Singleton
  static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
    return context.getSharedPreferences(SettingsPreferences.PREFS_NAME, Context.MODE_PRIVATE);
  }

  @Provides
  @Singleton
  static SettingsPreferences provideSettingsPreferences(SharedPreferences preferences) {
    return new SettingsPreferences(preferences);
  }
}
