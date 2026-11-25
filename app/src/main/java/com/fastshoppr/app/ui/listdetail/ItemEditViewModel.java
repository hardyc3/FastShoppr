/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.observation.Registration;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.usecase.MoveItemToDateUseCase;
import com.fastshoppr.app.domain.usecase.UpdateItemUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class ItemEditViewModel extends ViewModel {

  private static final int MAX_TEXT_LENGTH = 200;

  private final ListItemRepository itemRepository;
  private final StoreRepository storeRepository;
  private final SettingsRepository settingsRepository;
  private final ShoppingListRepository shoppingListRepository;
  private final UpdateItemUseCase updateItemUseCase;
  private final MoveItemToDateUseCase moveItemToDateUseCase;
  private final ExecutorService executor;
  private final SavedStateHandle savedStateHandle;

  private final String itemId;
  private final MutableLiveData<ItemEditUiState> state =
      new MutableLiveData<>(ItemEditUiState.loading());
  private final MutableLiveData<SingleEvent<ItemEditUiEvent>> events = new MutableLiveData<>();
  private final Registration itemRegistration;
  private final Registration storesRegistration;

  private ListItem item;
  private List<Store> stores = List.of();
  private boolean itemLoaded;
  private boolean storesLoaded;
  private String selectedStoreId;
  private LocalDate targetDate;

  @Inject
  public ItemEditViewModel(
      ListItemRepository itemRepository,
      StoreRepository storeRepository,
      SettingsRepository settingsRepository,
      ShoppingListRepository shoppingListRepository,
      UpdateItemUseCase updateItemUseCase,
      MoveItemToDateUseCase moveItemToDateUseCase,
      ExecutorService executor,
      SavedStateHandle savedStateHandle) {
    this.itemRepository = itemRepository;
    this.storeRepository = storeRepository;
    this.settingsRepository = settingsRepository;
    this.shoppingListRepository = shoppingListRepository;
    this.updateItemUseCase = updateItemUseCase;
    this.moveItemToDateUseCase = moveItemToDateUseCase;
    this.executor = executor;
    this.savedStateHandle = savedStateHandle;
    this.itemId = Objects.requireNonNull(savedStateHandle.get(NavArguments.ITEM_ID));
    this.itemRegistration = itemRepository.observeById(itemId).subscribe(this::onItemChanged);
    this.storesRegistration = storeRepository.observeActive().subscribe(this::onStoresChanged);
  }

  public LiveData<ItemEditUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<ItemEditUiEvent>> getEvents() {
    return events;
  }

  public void selectStore(String storeId) {
    selectedStoreId = storeId;
    rebuild();
  }

  public void selectDate(LocalDate date) {
    targetDate = date;
    rebuild();
  }

  public void save(String rawText, String displayName, String quantity, String aisle, String bay) {
    String trimmedRawText = rawText == null ? "" : rawText.trim();
    if (trimmedRawText.isEmpty()) {
      postError(UiError.RAW_TEXT_REQUIRED);
      return;
    }
    if (trimmedRawText.length() > MAX_TEXT_LENGTH) {
      postError(UiError.RAW_TEXT_TOO_LONG);
      return;
    }
    if (item == null) {
      postError(UiError.ITEM_NOT_FOUND);
      return;
    }

    CompletableFuture<ListItem> moved;
    if (targetDate.equals(item.getTargetDate())) {
      moved = CompletableFuture.completedFuture(item);
    } else {
      moved =
          CompletableFuture.supplyAsync(
                  () -> shoppingListRepository.getById(item.getListId()).getName(), executor)
              .thenCompose(
                  targetListName -> moveItemToDateUseCase.move(itemId, targetDate, targetListName));
    }

    moved
        .thenCompose(
            ignored ->
                updateItemUseCase.update(
                    itemId, trimmedRawText, displayName, quantity, selectedStoreId, aisle, bay))
        .whenComplete(
            (updated, error) -> {
              if (error != null) {
                postError(UiError.SAVE_FAILED);
                return;
              }
              settingsRepository.setLastUsedStoreId(selectedStoreId);
              events.postValue(new SingleEvent<>(new ItemEditUiEvent.Saved()));
            });
  }

  private void onItemChanged(ListItem listItem) {
    item = listItem;
    itemLoaded = true;
    if (listItem == null) {
      rebuild();
      return;
    }
    if (selectedStoreId == null) {
      selectedStoreId = listItem.getStoreId();
    }
    if (targetDate == null) {
      targetDate = listItem.getTargetDate();
    }
    rebuild();
  }

  private void onStoresChanged(List<Store> activeStores) {
    stores = activeStores == null ? List.of() : activeStores;
    storesLoaded = true;
    rebuild();
  }

  private void rebuild() {
    if (!itemLoaded || !storesLoaded) {
      state.setValue(ItemEditUiState.loading());
      return;
    }
    if (item == null) {
      state.setValue(
          new ItemEditUiState(
              false,
              null,
              null,
              null,
              null,
              null,
              null,
              stores,
              null,
              null,
              UiError.ITEM_NOT_FOUND));
      return;
    }

    String selectedStoreName = null;
    for (Store store : stores) {
      if (store.getId().equals(selectedStoreId)) {
        selectedStoreName = store.getName();
        break;
      }
    }

    state.setValue(
        new ItemEditUiState(
            false,
            item.getRawText(),
            item.getDisplayName(),
            item.getQuantity(),
            item.getAisle(),
            item.getBay(),
            targetDate,
            stores,
            selectedStoreId,
            selectedStoreName,
            null));
  }

  private void postError(UiError error) {
    events.postValue(new SingleEvent<>(new ItemEditUiEvent.ShowError(error)));
  }

  @Override
  protected void onCleared() {
    itemRegistration.close();
    storesRegistration.close();
  }
}
