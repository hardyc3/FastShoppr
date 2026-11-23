/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static org.junit.Assert.assertTrue;

import androidx.lifecycle.SavedStateHandle;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.AddItemUseCase;
import com.fastshoppr.app.domain.usecase.ApplyLookupCandidateUseCase;
import com.fastshoppr.app.domain.usecase.DeferItemUseCase;
import com.fastshoppr.app.domain.usecase.DeleteItemUseCase;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.domain.usecase.LookupItemUseCase;
import com.fastshoppr.app.domain.usecase.MoveItemToDateUseCase;
import com.fastshoppr.app.domain.usecase.ResolveDatePhraseUseCase;
import com.fastshoppr.app.domain.usecase.SetItemAisleUseCase;
import com.fastshoppr.app.domain.usecase.SortItemsUseCase;
import com.fastshoppr.app.domain.usecase.ToggleItemCheckedUseCase;
import com.fastshoppr.app.ui.listdetail.ListDetailRow;
import com.fastshoppr.app.ui.listdetail.ListDetailUiState;
import com.fastshoppr.app.ui.listdetail.ListDetailViewModel;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingScheduler;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class ListDetailStateRestorationTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject FastShopprDatabase database;
  @Inject ShoppingListRepository listRepository;
  @Inject ListItemRepository itemRepository;
  @Inject StoreRepository storeRepository;
  @Inject SettingsRepository settingsRepository;
  @Inject GetSelectedStoresUseCase getSelectedStoresUseCase;
  @Inject ExecutorService executorService;
  @Inject AddItemUseCase addItemUseCase;
  @Inject ToggleItemCheckedUseCase toggleItemCheckedUseCase;
  @Inject DeleteItemUseCase deleteItemUseCase;
  @Inject MoveItemToDateUseCase moveItemToDateUseCase;
  @Inject DeferItemUseCase deferItemUseCase;
  @Inject ResolveDatePhraseUseCase resolveDatePhraseUseCase;
  @Inject Clock clock;
  @Inject SetItemAisleUseCase setItemAisleUseCase;
  @Inject LookupItemUseCase lookupItemUseCase;
  @Inject ApplyLookupCandidateUseCase applyLookupCandidateUseCase;
  @Inject OrganizingScheduler organizingScheduler;
  @Inject SortItemsUseCase sortItemsUseCase;

  private String listId;
  private String walmartId;

  @Before
  public void setUp() {
    hiltRule.inject();
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM aisle_cache");
    ShoppingList list =
        listRepository.getOrCreate(LocalDate.of(2026, 11, 7), "Restore test", null, false);
    listId = list.getId();
    Store walmart = storeRepository.findByBrand(StoreBrand.WALMART);
    walmartId = walmart.getId();
    // List detail shows store sections only when a store has items (or is pending).
    addItemUseCase
        .add(
            listId, walmartId, "milk", null, null, com.fastshoppr.app.domain.model.ItemSource.TYPED)
        .join();
  }

  @Test
  public void draftSurvivesSimulatedProcessDeath() throws InterruptedException {
    Map<String, Object> firstSaved = new HashMap<>();
    firstSaved.put(NavArguments.LIST_ID, listId);
    SavedStateHandle firstHandle = new SavedStateHandle(firstSaved);
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ListDetailViewModel first = createViewModel(firstHandle);
              first.saveDraft(walmartId, "eggs");
            });

    Map<String, Object> secondSaved = new HashMap<>();
    secondSaved.put(NavArguments.LIST_ID, listId);
    secondSaved.put("draft:" + walmartId, firstHandle.get("draft:" + walmartId));

    AtomicReference<ListDetailUiState> restored = new AtomicReference<>();
    CountDownLatch loaded = new CountDownLatch(1);
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ListDetailViewModel second = createViewModel(new SavedStateHandle(secondSaved));
              second
                  .getState()
                  .observeForever(
                      state -> {
                        if (!state.isLoading() && !state.getRows().isEmpty()) {
                          restored.set(state);
                          loaded.countDown();
                        }
                      });
            });

    assertTrue(loaded.await(5, TimeUnit.SECONDS));
    boolean found =
        restored.get().getRows().stream()
            .anyMatch(
                row ->
                    row instanceof ListDetailRow.AddItem addItem
                        && addItem.store().getId().equals(walmartId)
                        && "eggs".equals(addItem.draftText()));
    assertTrue(found);
  }

  private ListDetailViewModel createViewModel(SavedStateHandle handle) {
    return new ListDetailViewModel(
        listRepository,
        itemRepository,
        storeRepository,
        settingsRepository,
        getSelectedStoresUseCase,
        executorService,
        addItemUseCase,
        toggleItemCheckedUseCase,
        deleteItemUseCase,
        moveItemToDateUseCase,
        deferItemUseCase,
        resolveDatePhraseUseCase,
        clock,
        setItemAisleUseCase,
        lookupItemUseCase,
        applyLookupCandidateUseCase,
        organizingScheduler,
        sortItemsUseCase,
        handle);
  }
}
