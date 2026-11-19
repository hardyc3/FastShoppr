/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingCounts;
import com.fastshoppr.app.work.OrganizingProgressStore;
import com.fastshoppr.app.work.OrganizingScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class LookupViewModel extends ViewModel {

  private final String listId;
  private final ListItemRepository itemRepository;
  private final StoreRepository storeRepository;
  private final SettingsRepository settingsRepository;
  private final GetSelectedStoresUseCase getSelectedStores;
  private final OrganizingScheduler scheduler;
  private final OrganizingProgressStore progressStore;
  private final ExecutorService executor;

  private final MutableLiveData<String> storeContext = new MutableLiveData<>("");
  private final MutableLiveData<Boolean> hasWalmart = new MutableLiveData<>(false);
  private final MutableLiveData<Boolean> walmartVerified = new MutableLiveData<>(false);
  private final MediatorLiveData<OrganizingProgressStore.Progress> progress =
      new MediatorLiveData<>();
  private final MutableLiveData<OrganizingCounts> counts = new MutableLiveData<>();
  private final MutableLiveData<String> perStoreDetails = new MutableLiveData<>("");

  @Inject
  public LookupViewModel(
      ListItemRepository itemRepository,
      StoreRepository storeRepository,
      SettingsRepository settingsRepository,
      GetSelectedStoresUseCase getSelectedStores,
      OrganizingScheduler scheduler,
      OrganizingProgressStore progressStore,
      ExecutorService executor,
      SavedStateHandle savedStateHandle) {
    this.itemRepository = itemRepository;
    this.storeRepository = storeRepository;
    this.settingsRepository = settingsRepository;
    this.getSelectedStores = getSelectedStores;
    this.scheduler = scheduler;
    this.progressStore = progressStore;
    this.executor = executor;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
    progress.addSource(progressStore.observe(), this::onProgressMap);
  }

  private void onProgressMap(Map<String, OrganizingProgressStore.Progress> map) {
    progress.setValue(map == null ? null : map.get(listId));
    // refresh counts/details as work progresses
    loadContext();
  }

  public String getListId() {
    return listId;
  }

  public LiveData<String> getStoreContext() {
    return storeContext;
  }

  public LiveData<Boolean> getHasWalmart() {
    return hasWalmart;
  }

  public LiveData<Boolean> getWalmartVerified() {
    return walmartVerified;
  }

  public LiveData<OrganizingProgressStore.Progress> getProgress() {
    return progress;
  }

  public LiveData<OrganizingCounts> getCounts() {
    return counts;
  }

  public LiveData<String> getPerStoreDetails() {
    return perStoreDetails;
  }

  public boolean isRunning() {
    OrganizingProgressStore.Progress p = progress.getValue();
    return p != null && p.running;
  }

  public void loadContext() {
    walmartVerified.postValue(settingsRepository.isWalmartStoreVerified());
    executor.execute(
        () -> {
          try {
            List<ListItem> items = itemRepository.getByList(listId);
            counts.postValue(OrganizingCounts.from(items));
            String context = "";
            StoreBrand baseBrand = null;
            boolean walmartPresent = false;
            Set<StoreBrand> presentBrands = new LinkedHashSet<>();
            StringBuilder details = new StringBuilder();
            java.util.Map<String, int[]> byStore = new java.util.LinkedHashMap<>();
            java.util.Map<String, String> storeNames = new java.util.LinkedHashMap<>();
            for (ListItem item : items) {
              Store store = resolveStoreForItem(item);
              if (store != null) {
                if (store.getBrand() == StoreBrand.WALMART) {
                  walmartPresent = true;
                }
                presentBrands.add(store.getBrand());
                if (context.isEmpty()) {
                  baseBrand = store.getBrand();
                  String addr = store.getAddressLine();
                  context = store.getName() + (addr == null || addr.isBlank() ? "" : " — " + addr);
                }
                storeNames.putIfAbsent(store.getId(), store.getName());
                int[] c = byStore.computeIfAbsent(store.getId(), k -> new int[5]);
                com.fastshoppr.app.domain.model.LookupStatus s = item.getLookupStatus();
                if (s == com.fastshoppr.app.domain.model.LookupStatus.FOUND) c[0]++;
                else if (s == com.fastshoppr.app.domain.model.LookupStatus.NEEDS_REVIEW) c[1]++;
                else if (s == com.fastshoppr.app.domain.model.LookupStatus.NOT_FOUND) c[2]++;
                else if (s == com.fastshoppr.app.domain.model.LookupStatus.ERROR) c[3]++;
                else c[4]++; // pending/queued/in-progress
              }
            }
            for (java.util.Map.Entry<String, int[]> e : byStore.entrySet()) {
              if (details.length() > 0) details.append("\n");
              String name = storeNames.getOrDefault(e.getKey(), "Store");
              int[] c = e.getValue();
              details
                  .append(name)
                  .append(": found ")
                  .append(c[0])
                  .append(", review ")
                  .append(c[1])
                  .append(", not found ")
                  .append(c[2])
                  .append(", errors ")
                  .append(c[3])
                  .append(", pending ")
                  .append(c[4]);
            }
            perStoreDetails.postValue(details.toString());
            hasWalmart.postValue(walmartPresent);
            storeContext.postValue(appendSelectedStores(context, baseBrand, presentBrands));
          } catch (Exception ignored) {
          }
        });
  }

  public void organizeNow() {
    scheduler.organizeNow(listId);
  }

  public void retryFailed() {
    scheduler.retryFailed(listId);
  }

  public void cancel() {
    scheduler.cancel(listId);
  }

  public boolean canPostNotifications() {
    return scheduler.canPostNotifications();
  }

  private String appendSelectedStores(
      String base, StoreBrand baseBrand, Set<StoreBrand> presentBrands) {
    Map<StoreBrand, NearbyStore> selected = getSelectedStores.execute();
    List<String> lines = new ArrayList<>();
    for (StoreBrand brand : presentBrands) {
      NearbyStore store = selected.get(brand);
      if (store != null) {
        lines.add(brand.getDisplayName() + ": " + store.getLabel());
      }
    }
    if (lines.isEmpty()) {
      return base;
    }
    StringBuilder result = new StringBuilder();
    if (!base.isEmpty() && (baseBrand == null || !selected.containsKey(baseBrand))) {
      result.append(base).append('\n');
    }
    result.append("Saved branches:\n").append(String.join("\n", lines));
    return result.toString();
  }

  private Store resolveStoreForItem(ListItem item) {
    try {
      if (item != null && item.getStoreId() != null) {
        return storeRepository.getById(item.getStoreId());
      }
    } catch (Exception ignored) {
    }
    try {
      String last = settingsRepository.getLastUsedStoreId();
      if (last != null) return storeRepository.getById(last);
    } catch (Exception ignored) {
    }
    try {
      String def = settingsRepository.getDefaultStoreId();
      if (def != null) return storeRepository.getById(def);
    } catch (Exception ignored) {
    }
    try {
      List<Store> active = storeRepository.getActive();
      if (active != null && !active.isEmpty()) return active.get(0);
    } catch (Exception ignored) {
    }
    return null;
  }
}
