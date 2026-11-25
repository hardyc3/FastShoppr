/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
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
import com.fastshoppr.app.di.PermissionModule;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.port.PermissionRequester;
import com.fastshoppr.app.domain.port.PermissionStatus;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.ui.common.PermissionLauncherBinder;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import dagger.hilt.components.SingletonComponent;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@UninstallModules(PermissionModule.class)
@RunWith(AndroidJUnit4.class)
public final class CaptureFlowsTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;
  @Inject ShoppingListRepository listRepository;
  @Inject CaptureRepository captureRepository;
  @Inject ListItemRepository listItemRepository;
  @Inject FakePermissionState permissionState;

  private String listId;

  @Before
  public void setUp() {
    hiltRule.inject();
    permissionState.setCameraGranted(true);
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
            List.of(new RecognizedLine("milk", 0.95f), new RecognizedLine("eggs", 0.2f)));
    captureRepository.insert(
        new CaptureSession("cap-1", listId, ItemSource.CAMERA_OCR, null, json, 1L, null, null));
  }

  @Test
  public void reviewFlowMergeSplitDeleteSave() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.list_detail, NavArguments.forList(listId));
      navigate(scenario, R.id.review, NavArguments.forCapture("cap-1"));

      waitForText("milk");
      waitForText("eggs");
      waitForText("Low confidence");
      waitFor(
          () ->
              onFocusedView(withId(R.id.recyclerView))
                  .check(matches(hasDescendant(withText("Walmart")))));
      onFocusedView(withId(R.id.reviewNote)).check(matches(isDisplayed()));
      onFocusedView(withId(R.id.btnAddLine)).check(matches(isDisplayed()));

      onFocusedView(withId(R.id.btnMergeSelected)).perform(click());
      waitForText("milk eggs");

      onFocusedView(withId(R.id.btnSplit)).perform(click());
      waitForText("milk");
      waitForText("eggs");

      onFocusedView(withId(R.id.checkSelectAll)).perform(click());
      onFocusedView(withId(R.id.recyclerView))
          .perform(RecyclerViewActions.actionOnItemAtPosition(1, clickRowCheckBox()));
      onFocusedView(withId(R.id.btnDeleteSelected)).perform(click());
      onFocusedView(withText("eggs")).check(doesNotExist());
      waitForText("milk");

      onFocusedView(withId(R.id.recyclerView))
          .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickRowCheckBox()));
      onFocusedView(withId(R.id.btnSave)).perform(click());

      waitForText("Weekly shop");
      List<ListItem> items = listItemRepository.getByList(listId);
      assertEquals(1, items.size());
      assertEquals("milk", items.get(0).getRawText());
      assertEquals(ItemSource.CAMERA_OCR, items.get(0).getSource());
      assertEquals(LocalDate.of(2026, 11, 8), items.get(0).getTargetDate());
      assertNotNull(items.get(0).getStoreId());
    }
  }

  @Test
  public void handwritingDraftReviewSavesAllLines() {
    String draft =
        captureRepository.encodeDraft(
            List.of(
                new CapturedLine("milk", null, true, 0.95f, false, null, null),
                new CapturedLine("eggs", null, true, 0.2f, false, null, null),
                new CapturedLine("apples", null, true, 0f, true, null, null)));
    captureRepository.insert(
        new CaptureSession("cap-ink", listId, ItemSource.DIGITAL_INK, null, draft, 2L, null, null));

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.list_detail, NavArguments.forList(listId));
      navigate(scenario, R.id.review, NavArguments.forCapture("cap-ink"));

      waitForText("milk");
      waitForText("eggs");
      waitForText("apples");
      onFocusedView(withId(R.id.btnAddLine)).check(matches(isDisplayed()));
      onFocusedView(withId(R.id.btnSave)).perform(click());

      waitForText("Weekly shop");
      List<ListItem> items = listItemRepository.getByList(listId);
      assertEquals(3, items.size());
      boolean found = false;
      for (ListItem item : items) {
        if ("apples".equals(item.getRawText())) {
          found = true;
          assertEquals(ItemSource.DIGITAL_INK, item.getSource());
        }
      }
      assertTrue(found);
    }
  }

  @Test
  public void cameraPermissionDeniedShowsRequestWithoutBlockingManualEntry() {
    permissionState.setCameraGranted(false);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.list_detail, NavArguments.forList(listId));
      waitFor(() -> onFocusedView(withId(R.id.organize_button)).check(matches(isDisplayed())));

      navigate(scenario, R.id.capture_camera, NavArguments.forList(listId));

      // These views render the permission state/events owned by the camera capture slice.
      waitForText(R.string.camera_permission_message);
      onFocusedView(withId(R.id.permissionPanel)).check(matches(isDisplayed()));
      onFocusedView(withId(R.id.btnAllowCamera)).check(matches(isDisplayed()));
      onFocusedView(withId(R.id.btnCapture)).check(matches(isDisplayed()));

      pressBackUnconditionally();
      waitFor(() -> onFocusedView(withId(R.id.organize_button)).check(matches(isDisplayed())));
    }
  }

  @Test
  public void cameraPermissionGrantedHidesPermissionRequest() {
    permissionState.setCameraGranted(true);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      navigate(scenario, R.id.list_detail, NavArguments.forList(listId));
      waitFor(() -> onFocusedView(withId(R.id.organize_button)).check(matches(isDisplayed())));

      navigate(scenario, R.id.capture_camera, NavArguments.forList(listId));

      // Granted state hides the request panel and keeps the capture action visible.
      waitFor(() -> onFocusedView(withId(R.id.permissionPanel)).check(matches(not(isDisplayed()))));
      onFocusedView(withId(R.id.btnAllowCamera)).check(matches(not(isDisplayed())));
      onFocusedView(withId(R.id.btnCapture)).check(matches(isDisplayed()));

      pressBackUnconditionally();
      waitFor(() -> onFocusedView(withId(R.id.organize_button)).check(matches(isDisplayed())));
    }
  }

  @Module
  @InstallIn(SingletonComponent.class)
  abstract static class FakePermissionModule {

    @Binds
    @Singleton
    abstract PermissionStatus bindPermissionStatus(FakePermissionState implementation);

    @Binds
    @Singleton
    abstract PermissionRequester bindPermissionRequester(FakePermissionRequester implementation);

    @Binds
    @Singleton
    abstract PermissionLauncherBinder bindPermissionLauncherBinder(
        FakePermissionRequester implementation);
  }

  @Singleton
  public static final class FakePermissionState implements PermissionStatus {

    private volatile boolean cameraGranted = true;

    @Inject
    FakePermissionState() {}

    void setCameraGranted(boolean granted) {
      cameraGranted = granted;
    }

    @Override
    public boolean isGranted(PlatformPermission permission) {
      return permission == PlatformPermission.CAMERA ? cameraGranted : true;
    }

    @Override
    public boolean shouldShowRationale(PlatformPermission permission) {
      return false;
    }
  }

  @Singleton
  public static final class FakePermissionRequester
      implements PermissionRequester, PermissionLauncherBinder {

    private final FakePermissionState state;

    @Inject
    FakePermissionRequester(FakePermissionState state) {
      this.state = state;
    }

    @Override
    public void request(PlatformPermission permission, Consumer<Boolean> result) {
      result.accept(state.isGranted(permission));
    }

    @Override
    public void bind(PlatformPermission permission, androidx.fragment.app.Fragment fragment) {}

    @Override
    public void unbind(PlatformPermission permission, androidx.fragment.app.Fragment fragment) {}
  }

  private static ViewAction clickRowCheckBox() {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isAssignableFrom(android.view.ViewGroup.class);
      }

      @Override
      public String getDescription() {
        return "click row checkbox";
      }

      @Override
      public void perform(UiController uiController, View view) {
        CheckBox checkBox = view.findViewById(R.id.checkBox);
        if (checkBox == null) {
          throw new AssertionError("row has no checkBox");
        }
        checkBox.performClick();
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

  private static void waitForText(int textRes) {
    waitFor(() -> onFocusedView(withText(textRes)).check(matches(isDisplayed())));
  }

  private static void waitForContentDescription(String description) {
    waitFor(() -> onFocusedView(withContentDescription(description)).check(matches(isDisplayed())));
  }

  private static void waitFor(Runnable assertion) {
    TestWait.waitFor(assertion);
  }
}
