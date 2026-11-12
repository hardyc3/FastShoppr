/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.data.remote.ai.GeminiNanoItemNameCleaner;
import com.fastshoppr.app.domain.ai.ItemNameCleaner;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.ai.ItemNameOutputValidator;
import com.fastshoppr.app.domain.ai.RuleBasedItemNameCleaner;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.google.gson.Gson;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class AiModule {

  @Binds
  @Singleton
  abstract ItemNameCleaner bindItemNameCleaner(GeminiNanoItemNameCleaner implementation);

  @Provides
  @Singleton
  static ItemNameOutputValidator provideItemNameOutputValidator(Gson gson) {
    return new ItemNameOutputValidator(gson);
  }

  @Provides
  @Singleton
  static ItemNameCleanerCoordinator provideItemNameCleanerCoordinator(
      ItemNameCleaner aiCleaner,
      RuleBasedItemNameCleaner rulesCleaner,
      SettingsRepository settingsRepository) {
    return new ItemNameCleanerCoordinator(aiCleaner, rulesCleaner, settingsRepository);
  }
}
