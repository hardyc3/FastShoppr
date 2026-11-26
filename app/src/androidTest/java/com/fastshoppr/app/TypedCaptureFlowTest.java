/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.os.Bundle;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.time.LocalDate;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class TypedCaptureFlowTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject ShoppingListRepository listRepository;
  @Inject CaptureRepository captureRepository;

  private String listId;

  @Before
  public void setUp() {
    hiltRule.inject();
    listId =
        listRepository.getOrCreate(LocalDate.of(2026, 11, 9), "Typed flow", null, false).getId();
  }

  @Test
  public void typedTextReachesReviewWithRealCaptureId() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.capture_typed, NavArguments.forList(listId));

      onView(withId(R.id.itemInput)).perform(replaceText("milk\n eggs"));
      onView(withId(R.id.btnContinue)).perform(click());

      String[] captureId = new String[1];
      TestWait.waitFor(
          () ->
              scenario.onActivity(
                  activity -> {
                    NavController controller = controller(activity);
                    assertNotNull(controller.getCurrentDestination());
                    assertEquals(R.id.review, controller.getCurrentDestination().getId());
                    Bundle arguments = controller.getCurrentBackStackEntry().getArguments();
                    assertNotNull(arguments);
                    captureId[0] = arguments.getString(NavArguments.CAPTURE_ID);
                    assertNotNull(captureId[0]);
                    assertFalse(NavArguments.PLACEHOLDER_CAPTURE_ID.equals(captureId[0]));
                  }));
      TestWait.waitFor(
          () -> {
            CaptureSession session = captureRepository.getById(captureId[0]);
            assertEquals(ItemSource.TYPED, session.getSource());
            List<CapturedLine> lines =
                captureRepository.decodeDraft(session.getRecognizedLinesJson());
            assertEquals(2, lines.size());
            assertEquals("milk", lines.get(0).getText());
            assertEquals("eggs", lines.get(1).getText());
          });
    }
  }

  @Test
  public void addLineReturnsToWritingScreenWithPendingEditsPreserved() {
    String json =
        captureRepository.encodeDraft(
            List.of(new CapturedLine("milk", null, true, 0.9f, false, null, null)));
    captureRepository.insert(
        new CaptureSession(
            "typed-flow-cap", listId, ItemSource.CAMERA_OCR, null, json, 1L, null, null));

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.review, NavArguments.forCapture("typed-flow-cap"));
      TestWait.waitFor(
          () ->
              scenario.onActivity(
                  activity -> {
                    NavController controller = controller(activity);
                    assertNotNull(controller.getCurrentDestination());
                    assertEquals(R.id.review, controller.getCurrentDestination().getId());
                  }));
      onView(withId(R.id.textField)).check(matches(isDisplayed()));
      onView(withId(R.id.textField)).perform(replaceText("edited milk"));

      onView(withId(R.id.btnAddLine)).check(matches(isDisplayed()));
      onView(withId(R.id.btnAddLine)).perform(click());

      TestWait.waitFor(
          () ->
              scenario.onActivity(
                  activity -> {
                    NavController controller = controller(activity);
                    assertNotNull(controller.getCurrentDestination());
                    assertEquals(R.id.capture_ink, controller.getCurrentDestination().getId());
                    Bundle arguments = controller.getCurrentBackStackEntry().getArguments();
                    assertNotNull(arguments);
                    assertEquals(listId, arguments.getString(NavArguments.LIST_ID));
                    assertEquals("typed-flow-cap", arguments.getString(NavArguments.CAPTURE_ID));
                  }));
      TestWait.waitFor(
          () -> {
            CaptureSession updated = captureRepository.getById("typed-flow-cap");
            List<CapturedLine> lines =
                captureRepository.decodeDraft(updated.getRecognizedLinesJson());
            assertEquals("edited milk", lines.get(0).getText());
          });
    }
  }

  private static void navigate(
      ActivityScenario<MainActivity> scenario, int destination, android.os.Bundle arguments) {
    scenario.onActivity(activity -> controller(activity).navigate(destination, arguments));
  }

  private static NavController controller(MainActivity activity) {
    NavHostFragment host =
        (NavHostFragment)
            activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    assertNotNull(host);
    return host.getNavController();
  }
}
