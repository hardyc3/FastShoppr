/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import com.fastshoppr.app.di.ClockModule;
import com.fastshoppr.app.domain.time.Clock;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

@Module
@TestInstallIn(components = SingletonComponent.class, replaces = ClockModule.class)
public abstract class FakeClockModule {

  @Binds
  abstract Clock bindClock(FakeClock implementation);
}
