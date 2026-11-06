/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.preferences.SettingsPreferences;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.testing.FakeClock;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class DependencyInjectionTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;

  @Inject SettingsPreferences settingsPreferences;

  @Inject ExecutorService executorService;

  @Inject Clock clock;

  @Before
  public void setUp() {
    hiltRule.inject();
  }

  @After
  public void tearDown() {
    executorService.shutdownNow();
  }

  @Test
  public void hiltProvidesDatabasePreferencesExecutorAndClock() {
    assertNotNull(database);
    assertNotNull(settingsPreferences);
    assertNotNull(executorService);
    assertNotNull(clock);
    assertEquals(FakeClock.NOW, clock.nowMillis());
  }
}
