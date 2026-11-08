/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.sort.AisleComparator;
import com.fastshoppr.app.domain.usecase.AddItemUseCase;
import com.fastshoppr.app.domain.usecase.DeleteItemUseCase;
import com.fastshoppr.app.domain.usecase.MoveItemToDateUseCase;
import com.fastshoppr.app.domain.usecase.SetItemAisleUseCase;
import com.fastshoppr.app.domain.usecase.SortItemsUseCase;
import com.fastshoppr.app.domain.usecase.ToggleItemCheckedUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

@HiltViewModel
public final class ListDetailViewModel extends ViewModel {

  private static final String KEY_SELECTED_STORE = "selected_store_id";
  private static final String KEY_DRAFT_PREFIX = "draft:";
  private static final int MAX_TEXT_LENGTH = 200;

  private final ShoppingListRepository listRepository;
  private final ListItemRepository itemRepository;
  private final StoreRepository storeRepository;
  private final SettingsRepository settingsRepository;
  private final AddItemUseCase addItemUseCase;
  private final ToggleItemCheckedUseCase toggleItemCheckedUseCase;
  private final DeleteItemUseCase deleteItemUseCase;
  private final MoveItemToDateUseCase moveItemToDateUseCase;
  private final SetItemAisleUseCase setItemAisleUseCase;
  private final SortItemsUseCase sortItemsUseCase;
  private final SavedStateHandle savedStateHandle;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private final String listId;
  private final LiveData<ShoppingList> listLiveData;
  private final LiveData<List<ListItem>> itemsLiveData;
  private final LiveData<List<Store>> storesLiveData;
  private final MutableLiveData<ListDetailUiState> state =
      new MutableLiveData<>(ListDetailUiState.loading());
  private final MutableLiveData<SingleEvent<ListDetailUiEvent>> events = new MutableLiveData<>();

  private final Observer<ShoppingList> listObserver = this::onListChanged;
  private final Observer<List<ListItem>> itemsObserver = this::onItemsChanged;
  private final Observer<List<Store>> storesObserver = this::onStoresChanged;

  private ShoppingList list;
  private List<ListItem> items = List.of();
  private List<Store> stores = List.of();
  private boolean listLoaded;
  private boolean itemsLoaded;
  private boolean storesLoaded;
  private SortDirection sortDirection;
  private UiError loadError;
  private long addGeneration;

  @Inject
  public ListDetailViewModel(
      ShoppingListRepository listRepository,
      ListItemRepository itemRepository,
      StoreRepository storeRepository,
      SettingsRepository settingsRepository,
      AddItemUseCase addItemUseCase,
      ToggleItemCheckedUseCase toggleItemCheckedUseCase,
      DeleteItemUseCase deleteItemUseCase,
      MoveItemToDateUseCase moveItemToDateUseCase,
      SetItemAisleUseCase setItemAisleUseCase,
      SortItemsUseCase sortItemsUseCase,
      SavedStateHandle savedStateHandle) {
    this.listRepository = listRepository;
    this.itemRepository = itemRepository;
    this.storeRepository = storeRepository;
    this.settingsRepository = settingsRepository;
    this.addItemUseCase = addItemUseCase;
    this.toggleItemCheckedUseCase = toggleItemCheckedUseCase;
    this.deleteItemUseCase = deleteItemUseCase;
    this.moveItemToDateUseCase = moveItemToDateUseCase;
    this.setItemAisleUseCase = setItemAisleUseCase;
    this.sortItemsUseCase = sortItemsUseCase;
    this.savedStateHandle = savedStateHandle;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
    this.sortDirection = settingsRepository.getSortDirection();
    this.listLiveData = listRepository.observeById(listId);
    this.itemsLiveData = itemRepository.observeByList(listId);
    this.storesLiveData = storeRepository.observeActive();

    listLiveData.observeForever(listObserver);
    itemsLiveData.observeForever(itemsObserver);
    storesLiveData.observeForever(storesObserver);
  }

  public LiveData<ListDetailUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<ListDetailUiEvent>> getEvents() {
    return events;
  }

  public void refresh() {
    rebuild();
  }

  public void toggleSortDirection() {
    sortDirection = sortDirection == SortDirection.ASC ? SortDirection.DESC : SortDirection.ASC;
    settingsRepository.setSortDirection(sortDirection);
    rebuild();
  }

  public void selectStore(String storeId) {
    savedStateHandle.set(KEY_SELECTED_STORE, storeId);
    settingsRepository.setLastUsedStoreId(storeId);
    rebuild();
  }

  public void saveDraft(String storeId, String text) {
    savedStateHandle.set(draftKey(storeId), text);
  }

  public void addItem(String storeId, String rawText) {
    String trimmed = rawText == null ? "" : rawText.trim();
    if (trimmed.isEmpty()) {
      postError(UiError.RAW_TEXT_REQUIRED);
      return;
    }
    if (trimmed.length() > MAX_TEXT_LENGTH) {
      postError(UiError.RAW_TEXT_TOO_LONG);
      return;
    }
    addItemUseCase
        .add(listId, storeId, trimmed, null, null, ItemSource.TYPED)
        .whenComplete(
            (item, error) -> {
              if (error != null) {
                postError(UiError.ADD_FAILED);
                return;
              }
              settingsRepository.setLastUsedStoreId(storeId);
              mainHandler.post(
                  () -> {
                    savedStateHandle.set(KEY_SELECTED_STORE, storeId);
                    savedStateHandle.remove(draftKey(storeId));
                    addGeneration++;
                    rebuild();
                  });
              postMessage(UiMessage.ITEM_ADDED);
            });
  }

  public void setChecked(String itemId, boolean checked) {
    toggleItemCheckedUseCase
        .setChecked(itemId, checked)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                postError(UiError.SAVE_FAILED);
              }
            });
  }

  public void deleteItem(String itemId) {
    deleteItemUseCase
        .delete(itemId)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                postError(UiError.DELETE_FAILED);
                return;
              }
              postMessage(UiMessage.ITEM_DELETED);
            });
  }

  public void setAisle(String itemId, String aisle, String bay) {
    setItemAisleUseCase
        .setAisle(itemId, aisle, bay)
        .whenComplete(
            (item, error) -> {
              if (error != null) {
                postError(UiError.SAVE_FAILED);
              }
            });
  }

  public void deferItem(String itemId, LocalDate targetDate, String targetListName) {
    moveItemToDateUseCase
        .move(itemId, targetDate, targetListName)
        .whenComplete(
            (item, error) -> {
              if (error != null) {
                postError(UiError.DEFER_FAILED);
                return;
              }
              postMessage(UiMessage.ITEM_DEFERRED);
            });
  }

  private void onListChanged(ShoppingList shoppingList) {
    list = shoppingList;
    listLoaded = true;
    loadError = shoppingList == null ? UiError.LIST_NOT_FOUND : null;
    rebuild();
  }

  private void onItemsChanged(List<ListItem> listItems) {
    items = listItems == null ? List.of() : listItems;
    itemsLoaded = true;
    rebuild();
  }

  private void onStoresChanged(List<Store> activeStores) {
    stores = activeStores == null ? List.of() : activeStores;
    storesLoaded = true;
    rebuild();
  }

  private void rebuild() {
    if (!listLoaded || !itemsLoaded || !storesLoaded) {
      state.setValue(ListDetailUiState.loading());
      return;
    }
    if (loadError != null || list == null) {
      state.setValue(
          new ListDetailUiState(
              false,
              null,
              null,
              List.of(),
              stores,
              sortDirection,
              null,
              null,
              loadError == null ? UiError.LIST_NOT_FOUND : loadError));
      return;
    }

    List<Store> orderedStores = new ArrayList<>(stores);
    orderedStores.sort(
        Comparator.comparingInt((Store store) -> store.getBrand().ordinal())
            .thenComparing(Store::getName, String.CASE_INSENSITIVE_ORDER));

    String selectedStoreId = resolveSelectedStoreId(orderedStores);
    savedStateHandle.set(KEY_SELECTED_STORE, selectedStoreId);
    String selectedStoreName = null;
    for (Store store : orderedStores) {
      if (store.getId().equals(selectedStoreId)) {
        selectedStoreName = store.getName();
        break;
      }
    }

    List<ListDetailRow> rows = new ArrayList<>();
    for (Store store : orderedStores) {
      List<ListItem> storeItems = new ArrayList<>();
      for (ListItem item : items) {
        if (item.getStoreId().equals(store.getId())) {
          storeItems.add(item);
        }
      }
      List<ListItem> sorted = sortItemsUseCase.sort(storeItems, sortDirection);
      rows.add(new ListDetailRow.StoreHeader(store, sorted.size()));

      List<ListItem> resolved = new ArrayList<>();
      List<ListItem> unresolved = new ArrayList<>();
      for (ListItem item : sorted) {
        if (AisleComparator.isUnresolved(item.getAisle())) {
          unresolved.add(item);
        } else {
          resolved.add(item);
        }
      }
      resolved.forEach(item -> rows.add(new ListDetailRow.Item(item)));
      if (!unresolved.isEmpty()) {
        rows.add(new ListDetailRow.AttentionHeader(store.getId()));
        unresolved.forEach(item -> rows.add(new ListDetailRow.Item(item)));
      }
      rows.add(new ListDetailRow.AddItem(store, getDraft(store.getId()), addGeneration));
    }

    state.setValue(
        new ListDetailUiState(
            false,
            list.getName(),
            list.getShoppingDate(),
            rows,
            orderedStores,
            sortDirection,
            selectedStoreId,
            selectedStoreName,
            null));
  }

  private String resolveSelectedStoreId(List<Store> orderedStores) {
    String saved = savedStateHandle.get(KEY_SELECTED_STORE);
    if (containsStore(orderedStores, saved)) {
      return saved;
    }
    String lastUsed = settingsRepository.getLastUsedStoreId();
    if (containsStore(orderedStores, lastUsed)) {
      return lastUsed;
    }
    String defaultStore = settingsRepository.getDefaultStoreId();
    if (containsStore(orderedStores, defaultStore)) {
      return defaultStore;
    }
    for (Store store : orderedStores) {
      if (store.getBrand() == StoreBrand.WALMART) {
        return store.getId();
      }
    }
    return orderedStores.isEmpty() ? null : orderedStores.get(0).getId();
  }

  private static boolean containsStore(List<Store> stores, String storeId) {
    if (storeId == null) {
      return false;
    }
    for (Store store : stores) {
      if (store.getId().equals(storeId)) {
        return true;
      }
    }
    return false;
  }

  private String getDraft(String storeId) {
    String value = savedStateHandle.get(draftKey(storeId));
    return value == null ? "" : value;
  }

  private static String draftKey(String storeId) {
    return KEY_DRAFT_PREFIX + storeId;
  }

  private void postError(UiError error) {
    events.postValue(new SingleEvent<>(new ListDetailUiEvent.ShowError(error)));
  }

  private void postMessage(UiMessage message) {
    events.postValue(new SingleEvent<>(new ListDetailUiEvent.ShowMessage(message)));
  }

  @Override
  protected void onCleared() {
    listLiveData.removeObserver(listObserver);
    itemsLiveData.removeObserver(itemsObserver);
    storesLiveData.removeObserver(storesObserver);
  }
}
