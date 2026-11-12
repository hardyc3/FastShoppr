/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import com.fastshoppr.app.di.AiModule;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.ai.RuleBasedItemNameCleaner;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;
import javax.inject.Singleton;

@Module
@TestInstallIn(components = SingletonComponent.class, replaces = AiModule.class)
public final class FakeAiModule {

  private FakeAiModule() {}

  @Provides
  @Singleton
  static ItemNameCleanerCoordinator provideCoordinator(SettingsRepository settingsRepository) {
    return new ItemNameCleanerCoordinator(
        new FakeItemNameCleaner(), new RuleBasedItemNameCleaner(), settingsRepository);
  }
}
