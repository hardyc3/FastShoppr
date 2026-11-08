/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.annotation.IdRes;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class NavigationGraphTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Test
  public void everyPlaceholderDestinationIsReachable() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            NavHostFragment host =
                (NavHostFragment)
                    activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            assertNotNull(host);

            NavController navController = host.getNavController();
            assertDestination(navController, R.id.lists);

            navController.navigate(R.id.list_detail, NavArguments.forList("list-1"));
            assertDestination(navController, R.id.list_detail);

            navController.navigate(R.id.capture_typed, NavArguments.forList("list-1"));
            assertDestination(navController, R.id.capture_typed);

            navController.navigate(R.id.capture_ink, NavArguments.forList("list-1"));
            assertDestination(navController, R.id.capture_ink);

            navController.navigate(R.id.capture_camera, NavArguments.forList("list-1"));
            assertDestination(navController, R.id.capture_camera);

            navController.navigate(R.id.review, NavArguments.forCapture("capture-1"));
            assertDestination(navController, R.id.review);

            navController.navigate(R.id.lookup, NavArguments.forList("list-1"));
            assertDestination(navController, R.id.lookup);

            navController.navigate(R.id.calendar);
            assertDestination(navController, R.id.calendar);

            navController.navigate(R.id.settings);
            assertDestination(navController, R.id.settings);
          });
    }
  }

  private static void assertDestination(NavController navController, @IdRes int destinationId) {
    assertNotNull(navController.getCurrentDestination());
    assertEquals(destinationId, navController.getCurrentDestination().getId());
  }
}
