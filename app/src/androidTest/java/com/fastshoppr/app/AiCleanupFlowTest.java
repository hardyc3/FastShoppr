/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.time.LocalDate;
import java.util.List;
import javax.inject.Inject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class AiCleanupFlowTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;
  @Inject ShoppingListRepository listRepository;
  @Inject CaptureRepository captureRepository;
  @Inject ListItemRepository listItemRepository;
  @Inject SettingsRepository settingsRepository;

  private String listId;

  @Before
  public void setUp() {
    hiltRule.inject();
    settingsRepository.setAiCleanupEnabled(false);
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM aisle_cache");

    ShoppingList list =
        listRepository.getOrCreate(LocalDate.of(2026, 11, 8), "Weekly shop", null, false);
    listId = list.getId();
    String json =
        captureRepository.encodeRecognizedLines(
            List.of(
                new RecognizedLine("tp", 0.95f),
                new RecognizedLine("pb", 0.95f),
                new RecognizedLine("gal", 0.95f)));
    captureRepository.insert(
        new CaptureSession("cap-ai-1", listId, ItemSource.CAMERA_OCR, null, json, 1L, null, null));
  }

  @After
  public void tearDown() {
    settingsRepository.setAiCleanupEnabled(false);
  }

  @Test
  public void reviewCleanupAcceptEditDismissKeepsRawText() {
    settingsRepository.setAiCleanupEnabled(true);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.list_detail, NavArguments.forList(listId));
      navigate(scenario, R.id.review, NavArguments.forCapture("cap-ai-1"));

      waitForText("tp");
      waitFor(() -> onFocusedView(withId(R.id.btnCleanup)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.btnCleanup)).perform(click());
      waitForText("After: Toilet paper");

      onFocusedView(withId(R.id.suggestionList))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(withText("After: Toilet paper")), clickRowButton(R.id.btnAccept)));
      onFocusedView(withId(R.id.suggestionList))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(withText("After: Peanut butter")), clickRowButton(R.id.btnEdit)));
      waitFor(() -> onFocusedView(isAssignableFrom(EditText.class)).check(matches(isDisplayed())));
      onFocusedView(isAssignableFrom(EditText.class)).perform(replaceText("Crunchy peanut butter"));
      onFocusedView(withId(android.R.id.button1)).perform(click());
      onFocusedView(withId(R.id.suggestionList))
          .perform(
              RecyclerViewActions.actionOnItem(
                  hasDescendant(withText("After: Gallon")), clickRowButton(R.id.btnDismiss)));
      onFocusedView(withId(R.id.btnCloseCleanup)).perform(click());

      waitForText("Name: Toilet paper");
      waitForText("Name: Crunchy peanut butter");
      onFocusedView(withText("Name: Gallon")).check(doesNotExist());

      onFocusedView(withId(R.id.btnSave)).perform(click());
      waitForText("Weekly shop");

      List<ListItem> items = listItemRepository.getByList(listId);
      assertEquals(3, items.size());
      ListItem toiletPaper = itemWithRawText(items, "tp");
      assertNotNull(toiletPaper);
      assertEquals("Toilet paper", toiletPaper.getDisplayName());
      ListItem peanutButter = itemWithRawText(items, "pb");
      assertNotNull(peanutButter);
      assertEquals("Crunchy peanut butter", peanutButter.getDisplayName());
      ListItem gallon = itemWithRawText(items, "gal");
      assertNotNull(gallon);
      assertEquals("gal", gallon.getDisplayName());
    }
  }

  @Test
  public void settingsTogglePersistsAcrossScreens() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      waitFor(() -> onFocusedView(withId(R.id.open_settings_button)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.open_settings_button)).perform(click());
      waitFor(() -> onFocusedView(withId(R.id.switchAiCleanup)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.switchAiCleanup)).check(matches(not(isChecked())));
      onFocusedView(withId(R.id.switchAiCleanup)).perform(scrollTo(), click());
      onFocusedView(withId(R.id.switchAiCleanup)).check(matches(isChecked()));
      assertTrue(settingsRepository.isAiCleanupEnabled());

      pressBackUnconditionally();
      waitFor(() -> onFocusedView(withId(R.id.open_settings_button)).check(matches(isDisplayed())));
      onFocusedView(withId(R.id.open_settings_button)).perform(click());
      waitFor(() -> onFocusedView(withId(R.id.switchAiCleanup)).check(matches(isChecked())));
    }
  }

  private static ListItem itemWithRawText(List<ListItem> items, String rawText) {
    for (ListItem item : items) {
      if (rawText.equals(item.getRawText())) {
        return item;
      }
    }
    return null;
  }

  private static ViewAction clickRowButton(int viewId) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isAssignableFrom(ViewGroup.class);
      }

      @Override
      public String getDescription() {
        return "click row button " + viewId;
      }

      @Override
      public void perform(UiController uiController, View view) {
        View button = view.findViewById(viewId);
        if (button == null) {
          throw new AssertionError("row has no button " + viewId);
        }
        button.performClick();
        uiController.loopMainThreadUntilIdle();
      }
    };
  }

  private static void navigate(
      ActivityScenario<MainActivity> scenario, int destinationId, Bundle args) {
    scenario.onActivity(
        activity -> {
          NavHostFragment host =
              (NavHostFragment)
                  activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
          assertNotNull(host);
          NavController navController = host.getNavController();
          navController.navigate(destinationId, args);
        });
  }

  private static ViewInteraction onFocusedView(Matcher<View> matcher) {
    return androidx.test.espresso.Espresso.onView(allOf(matcher, inFocusedWindow()));
  }

  private static Matcher<View> inFocusedWindow() {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(View view) {
        return view.hasWindowFocus();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("view is in the focused window");
      }
    };
  }

  private static void waitForText(String text) {
    waitFor(() -> onFocusedView(withText(text)).check(matches(isDisplayed())));
  }

  private static void waitFor(Runnable assertion) {
    TestWait.waitFor(assertion, 10_000L);
  }
}
