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
import com.fastshoppr.app.domain.date.LocalDateTimes;
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
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.AddItemUseCase;
import com.fastshoppr.app.domain.usecase.ApplyLookupCandidateUseCase;
import com.fastshoppr.app.domain.usecase.DeferItemUseCase;
import com.fastshoppr.app.domain.usecase.DeleteItemUseCase;
import com.fastshoppr.app.domain.usecase.LookupItemUseCase;
import com.fastshoppr.app.domain.usecase.MoveItemToDateUseCase;
import com.fastshoppr.app.domain.usecase.ResolveDatePhraseUseCase;
import com.fastshoppr.app.domain.usecase.SetItemAisleUseCase;
import com.fastshoppr.app.domain.usecase.SortItemsUseCase;
import com.fastshoppr.app.domain.usecase.ToggleItemCheckedUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StoreOrdering;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

@HiltViewModel
public final class ListDetailViewModel extends ViewModel {

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
  private final DeferItemUseCase deferItemUseCase;
  private final ResolveDatePhraseUseCase resolveDatePhraseUseCase;
  private final Clock clock;
  private final SetItemAisleUseCase setItemAisleUseCase;
  private final LookupItemUseCase lookupItemUseCase;
  private final ApplyLookupCandidateUseCase applyLookupCandidateUseCase;
  private final OrganizingScheduler organizingScheduler;
  private final ListDetailRowsBuilder rowsBuilder;
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
  private String pendingStoreId;
  private Store pendingStore;
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
      DeferItemUseCase deferItemUseCase,
      ResolveDatePhraseUseCase resolveDatePhraseUseCase,
      Clock clock,
      SetItemAisleUseCase setItemAisleUseCase,
      LookupItemUseCase lookupItemUseCase,
      ApplyLookupCandidateUseCase applyLookupCandidateUseCase,
      OrganizingScheduler organizingScheduler,
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
    this.deferItemUseCase = deferItemUseCase;
    this.resolveDatePhraseUseCase = resolveDatePhraseUseCase;
    this.clock = clock;
    this.setItemAisleUseCase = setItemAisleUseCase;
    this.lookupItemUseCase = lookupItemUseCase;
    this.applyLookupCandidateUseCase = applyLookupCandidateUseCase;
    this.organizingScheduler = organizingScheduler;
    this.rowsBuilder = new ListDetailRowsBuilder(sortItemsUseCase);
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
    java.time.LocalDate today = LocalDateTimes.today(clock);
    java.time.LocalDate currentListDate = list != null ? list.getShoppingDate() : today;
    resolveDatePhraseUseCase
        .resolve(trimmed, currentListDate, today)
        .whenComplete(
            (opt, err) -> {
              if (err == null && opt != null && opt.isPresent()) {
                var r = opt.get();
                if (r.resolvedDate().isPresent()) {
                  var p = r.parsed();
                  java.time.LocalDate targetListDate = r.resolvedDate().get();
                  String existingTargetListId = null;
                  if (r.nearestList().isPresent()
                      && r.nearestList().get().existingList().isPresent()) {
                    existingTargetListId = r.nearestList().get().existingList().get().getId();
                    targetListDate = r.nearestList().get().existingList().get().getShoppingDate();
                  } else if (r.nearestList().isPresent()) {
                    targetListDate = r.nearestList().get().targetDate();
                  }
                  boolean needsConfirm =
                      p.needsConfirmation()
                          || (r.nearestList().isPresent()
                              && r.nearestList().get().needsConfirmation());
                  String note = p.note();
                  if (r.nearestList().isPresent() && r.nearestList().get().needsConfirmation()) {
                    note =
                        (note == null || note.isBlank())
                            ? "Resolved date is before today."
                            : note + " Resolved date is before today.";
                  }
                  events.postValue(
                      new SingleEvent<>(
                          new ListDetailUiEvent.RequestDeferral(
                              storeId,
                              trimmed,
                              p.itemText(),
                              p.phrase(),
                              r.resolvedDate().get(),
                              targetListDate,
                              existingTargetListId,
                              needsConfirm,
                              note)));
                  return;
                }
                if (r.requiresSchedule() || (r.parsed() != null && r.parsed().requiresSchedule())) {
                  var p = r.parsed();
                  events.postValue(
                      new SingleEvent<>(
                          new ListDetailUiEvent.NeedsScheduleDate(
                              storeId,
                              trimmed,
                              p == null ? trimmed : p.itemText(),
                              p == null ? "" : p.phrase())));
                  return;
                }
              }
              addItemNow(storeId, trimmed);
            });
  }

  public void confirmDeferredAdd(
      String storeId,
      String originalText,
      String itemTextWithoutPhrase,
      java.time.LocalDate targetListDate,
      String existingTargetListId,
      boolean removePhrase) {
    String itemText = removePhrase ? itemTextWithoutPhrase : originalText;
    String finalText = itemText == null ? originalText : itemText.trim();
    if (finalText.isEmpty()) finalText = originalText;
    String listName = (list != null && list.getName() != null) ? list.getName() : "Shopping list";
    final java.time.LocalDate date = targetListDate;
    final String existingId = existingTargetListId;
    final String finalStoreId = storeId;
    final String finalItemText = finalText;
    new Thread(
            () -> {
              try {
                com.fastshoppr.app.domain.model.ShoppingList targetList;
                if (existingId != null && !existingId.isBlank()) {
                  targetList = listRepository.getById(existingId);
                } else {
                  targetList = listRepository.getOrCreate(date, listName, null, false);
                }
                addItemUseCase
                    .add(
                        targetList.getId(),
                        finalStoreId,
                        finalItemText,
                        null,
                        null,
                        ItemSource.TYPED)
                    .whenComplete(
                        (item, error) -> {
                          if (error != null) {
                            postError(UiError.DEFER_FAILED);
                            return;
                          }
                          settingsRepository.setLastUsedStoreId(finalStoreId);
                          mainHandler.post(
                              () -> {
                                savedStateHandle.remove(draftKey(finalStoreId));
                                addGeneration++;
                                rebuild();
                              });
                          postMessage(UiMessage.ITEM_DEFERRED);
                        });
              } catch (Exception e) {
                postError(UiError.DEFER_FAILED);
              }
            })
        .start();
  }

  private void addItemNow(String storeId, String trimmed) {
    addItemUseCase
        .add(listId, storeId, trimmed, null, null, ItemSource.TYPED)
        .whenComplete(
            (item, error) -> {
              if (error != null) {
                postError(UiError.ADD_FAILED);
                return;
              }
              settingsRepository.setLastUsedStoreId(storeId);
              if (organizingScheduler != null) {
                organizingScheduler.scheduleAfterSave(listId);
              }
              mainHandler.post(
                  () -> {
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

  public void refreshLookup(String itemId) {
    lookupItemUseCase
        .lookup(itemId, true)
        .whenComplete(
            (outcome, error) -> {
              if (error != null) {
                postError(UiError.SAVE_FAILED);
              }
            });
  }

  /** Applies a user-confirmed lookup candidate shown from a persisted Multiple result. */
  public void applyLookupCandidate(
      String itemId, com.fastshoppr.app.domain.lookup.StoreLookupResult.Found candidate) {
    applyLookupCandidateUseCase
        .apply(itemId, candidate)
        .whenComplete(
            (item, error) -> {
              if (error != null) {
                postError(UiError.SAVE_FAILED);
                return;
              }
              postMessage(UiMessage.AISLE_SAVED);
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
              loadError == null ? UiError.LIST_NOT_FOUND : loadError));
      return;
    }

    List<Store> sourceStores = stores;
    if (pendingStoreId != null && pendingStore != null) {
      boolean present = false;
      for (Store s : sourceStores) {
        if (pendingStoreId.equals(s.getId())) {
          present = true;
          break;
        }
      }
      if (!present) {
        List<Store> merged = new ArrayList<>(sourceStores);
        merged.add(pendingStore);
        sourceStores = merged;
      }
    }
    List<Store> orderedStores = StoreOrdering.byBrandThenName(sourceStores);

    // Show stores with items, plus pending/added store, plus default/last-used (up to 3) for
    // auto-created/empty lists.
    java.util.Set<String> included = new java.util.LinkedHashSet<>();
    List<Store> storesWithItems = new ArrayList<>();
    boolean pendingStillEmpty = false;
    for (Store store : orderedStores) {
      boolean hasItems = false;
      for (ListItem item : items) {
        if (item.getStoreId().equals(store.getId())) {
          hasItems = true;
          break;
        }
      }
      boolean isPending = store.getId().equals(pendingStoreId);
      if (hasItems || isPending) {
        storesWithItems.add(store);
        included.add(store.getId());
      }
      if (isPending && !hasItems) pendingStillEmpty = true;
    }
    if (items.isEmpty() && list != null && list.isAutoCreated()) {
      String def = settingsRepository.getDefaultStoreId();
      String last = settingsRepository.getLastUsedStoreId();
      String[] pref = new String[] {def, last};
      for (String id : pref) {
        if (id == null || included.contains(id) || included.size() >= 3) continue;
        for (Store s : orderedStores) {
          if (id.equals(s.getId())) {
            storesWithItems.add(s);
            included.add(s.getId());
            break;
          }
        }
      }
      if (storesWithItems.size() < 3) {
        for (Store s : orderedStores) {
          if (included.size() >= 3) break;
          if (!included.contains(s.getId())) {
            storesWithItems.add(s);
            included.add(s.getId());
          }
        }
      }
    }
    if (!pendingStillEmpty) {
      pendingStoreId = null;
      pendingStore = null;
    }

    List<ListDetailRow> rows =
        rowsBuilder.build(storesWithItems, items, sortDirection, this::getDraft, addGeneration);

    state.setValue(
        new ListDetailUiState(
            false,
            list.getName(),
            list.getShoppingDate(),
            rows,
            storesWithItems,
            sortDirection,
            null));
  }

  /** Adds a new store (custom or supported brand) and shows it in the list. */
  public void addStore(String name) {
    String trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty()) {
      postError(UiError.RAW_TEXT_REQUIRED);
      return;
    }
    Store store =
        new Store(
            java.util.UUID.randomUUID().toString(),
            StoreBrand.CUSTOM,
            trimmed,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            false,
            null,
            System.currentTimeMillis(),
            System.currentTimeMillis());
    new Thread(
            () -> {
              try {
                storeRepository.save(store);
                mainHandler.post(
                    () -> {
                      pendingStoreId = store.getId();
                      pendingStore = store;
                      rebuild();
                    });
              } catch (Exception e) {
                postError(UiError.SAVE_FAILED);
              }
            })
        .start();
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
