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
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.PermissionAction;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorPages;
import com.fastshoppr.app.domain.notification.NotificationPermissionStatus;
import com.fastshoppr.app.domain.port.PermissionRequester;
import com.fastshoppr.app.domain.port.PermissionStatus;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.domain.usecase.RequestPermissionUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingCounts;
import com.fastshoppr.app.work.OrganizingProgressStore;
import com.fastshoppr.app.work.OrganizingScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class LookupViewModel extends ViewModel {

  private static final PlatformPermission PERMISSION = PlatformPermission.POST_NOTIFICATIONS;
  private static final String KEY_PERMISSION_PREVIOUSLY_REQUESTED = "lookupPermissionRequested";
  private static final String KEY_RATIONALE_ACKNOWLEDGED = "lookupRationaleAcknowledged";

  private final String listId;
  private final ListItemRepository itemRepository;
  private final StoreRepository storeRepository;
  private final SettingsRepository settingsRepository;
  private final GetSelectedStoresUseCase getSelectedStores;
  private final StoreContextFormatter storeContextFormatter;
  private final OrganizingScheduler scheduler;
  private final OrganizingProgressStore progressStore;
  private final ExecutorService executor;
  private final NotificationPermissionStatus notificationPermissionStatus;
  private final PermissionStatus permissionStatus;
  private final PermissionRequester permissionRequester;
  private final RequestPermissionUseCase permissionResolver;
  private final SavedStateHandle savedStateHandle;

  private final MutableLiveData<String> storeContext = new MutableLiveData<>("");
  private final MutableLiveData<StoreBrand> missingBranch = new MutableLiveData<>(null);
  private final MutableLiveData<Boolean> branchesReady = new MutableLiveData<>(true);
  private final MutableLiveData<Boolean> branchCheckDone = new MutableLiveData<>(false);
  private final MediatorLiveData<OrganizingProgressStore.Progress> progress =
      new MediatorLiveData<>();
  private final MutableLiveData<OrganizingCounts> counts = new MutableLiveData<>();
  private final MutableLiveData<String> perStoreDetails = new MutableLiveData<>("");
  private final MutableLiveData<SingleEvent<PermissionAction>> permissionActions =
      new MutableLiveData<>();

  private boolean permissionPreviouslyRequested;
  private boolean rationaleAcknowledged;
  private boolean lastPermissionGranted;
  private boolean lastShouldShowRationale;

  @Inject
  public LookupViewModel(
      ListItemRepository itemRepository,
      StoreRepository storeRepository,
      SettingsRepository settingsRepository,
      GetSelectedStoresUseCase getSelectedStores,
      StoreContextFormatter storeContextFormatter,
      OrganizingScheduler scheduler,
      OrganizingProgressStore progressStore,
      ExecutorService executor,
      NotificationPermissionStatus notificationPermissionStatus,
      PermissionStatus permissionStatus,
      PermissionRequester permissionRequester,
      RequestPermissionUseCase permissionResolver,
      SavedStateHandle savedStateHandle) {
    this.itemRepository = itemRepository;
    this.storeRepository = storeRepository;
    this.settingsRepository = settingsRepository;
    this.getSelectedStores = getSelectedStores;
    this.storeContextFormatter = storeContextFormatter;
    this.scheduler = scheduler;
    this.progressStore = progressStore;
    this.executor = executor;
    this.notificationPermissionStatus = notificationPermissionStatus;
    this.permissionStatus = permissionStatus;
    this.permissionRequester = permissionRequester;
    this.permissionResolver = permissionResolver;
    this.savedStateHandle = savedStateHandle;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
    permissionPreviouslyRequested =
        Boolean.TRUE.equals(savedStateHandle.get(KEY_PERMISSION_PREVIOUSLY_REQUESTED));
    rationaleAcknowledged = Boolean.TRUE.equals(savedStateHandle.get(KEY_RATIONALE_ACKNOWLEDGED));
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

  public LiveData<StoreBrand> getMissingBranch() {
    return missingBranch;
  }

  public LiveData<Boolean> getBranchesReady() {
    return branchesReady;
  }

  public LiveData<Boolean> getBranchCheckDone() {
    return branchCheckDone;
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

  public LiveData<SingleEvent<PermissionAction>> getPermissionActions() {
    return permissionActions;
  }

  /** Reads notification permission through the domain port before starting organization. */
  public void checkNotificationPermission() {
    if (!notificationPermissionStatus.isRuntimePermissionRequired()) {
      organizeNow();
      return;
    }
    checkPermission(
        permissionStatus.isGranted(PERMISSION), permissionStatus.shouldShowRationale(PERMISSION));
  }

  private void checkPermission(boolean granted, boolean shouldShowRationale) {
    lastPermissionGranted = granted;
    lastShouldShowRationale = shouldShowRationale;
    boolean permanentlyDenied = !granted && permissionPreviouslyRequested && !shouldShowRationale;
    boolean rationaleDone = rationaleAcknowledged;
    PermissionAction action = permissionResolver.resolve(granted, rationaleDone, permanentlyDenied);
    if (action == PermissionAction.REQUEST) {
      permissionRequester.request(PERMISSION, this::permissionResult);
      return;
    }
    if (action == PermissionAction.NONE || action == PermissionAction.SETTINGS) {
      organizeNow();
    }
    permissionActions.setValue(new SingleEvent<>(action));
  }

  /** Continues without notification prompts when the user declines the rationale. */
  public void permissionRationaleDeclined() {
    organizeNow();
  }

  public void rationaleAccepted() {
    rationaleAcknowledged = true;
    savedStateHandle.set(KEY_RATIONALE_ACKNOWLEDGED, true);
    checkPermission(lastPermissionGranted, lastShouldShowRationale);
  }

  public void permissionResult(boolean granted) {
    permissionPreviouslyRequested = true;
    savedStateHandle.set(KEY_PERMISSION_PREVIOUSLY_REQUESTED, true);
    rationaleAcknowledged = false;
    savedStateHandle.set(KEY_RATIONALE_ACKNOWLEDGED, false);
    checkPermission(granted, permissionStatus.shouldShowRationale(PERMISSION));
  }

  public boolean isRunning() {
    OrganizingProgressStore.Progress p = progress.getValue();
    return p != null && p.running;
  }

  public void loadContext() {
    branchCheckDone.postValue(false);
    executor.execute(
        () -> {
          try {
            List<ListItem> items = itemRepository.getByList(listId);
            counts.postValue(OrganizingCounts.from(items));
            String context = "";
            StoreBrand baseBrand = null;
            Set<StoreBrand> presentBrands = new LinkedHashSet<>();
            StringBuilder details = new StringBuilder();
            Map<String, int[]> byStore = new LinkedHashMap<>();
            Map<String, String> storeNames = new LinkedHashMap<>();
            for (ListItem item : items) {
              Store store = resolveStoreForItem(item);
              if (store != null) {
                presentBrands.add(store.getBrand());
                if (context.isEmpty()) {
                  baseBrand = store.getBrand();
                  String addr = store.getAddressLine();
                  context = store.getName() + (addr == null || addr.isBlank() ? "" : " — " + addr);
                }
                storeNames.putIfAbsent(store.getId(), store.getName());
                int[] c = byStore.computeIfAbsent(store.getId(), k -> new int[5]);
                LookupStatus s = item.getLookupStatus();
                if (s == LookupStatus.FOUND) c[0]++;
                else if (s == LookupStatus.NEEDS_REVIEW) c[1]++;
                else if (s == LookupStatus.NOT_FOUND) c[2]++;
                else if (s == LookupStatus.ERROR) c[3]++;
                else c[4]++; // pending/queued/in-progress
              }
            }
            for (Map.Entry<String, int[]> e : byStore.entrySet()) {
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
            storeContext.postValue(appendSelectedStores(context, baseBrand, presentBrands));
            publishBranchRequirement(presentBrands);
          } catch (Exception ignored) {
            branchCheckDone.postValue(true);
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

  private void publishBranchRequirement(Set<StoreBrand> presentBrands) {
    Map<StoreBrand, NearbyStore> selected = getSelectedStores.execute();
    for (StoreBrand brand : StoreLocatorPages.brandOrder()) {
      if (presentBrands.contains(brand) && !selected.containsKey(brand)) {
        missingBranch.postValue(brand);
        branchesReady.postValue(false);
        return;
      }
    }
    missingBranch.postValue(null);
    branchesReady.postValue(true);
    branchCheckDone.postValue(true);
  }

  private String appendSelectedStores(
      String base, StoreBrand baseBrand, Set<StoreBrand> presentBrands) {
    Map<StoreBrand, NearbyStore> selected = getSelectedStores.execute();
    List<String> lines = new ArrayList<>();
    for (StoreBrand brand : presentBrands) {
      NearbyStore store = selected.get(brand);
      if (store != null) {
        lines.add(storeContextFormatter.savedBranchLine(brand, store.getLabel()));
      }
    }
    if (lines.isEmpty()) {
      return base;
    }
    StringBuilder result = new StringBuilder();
    if (!base.isEmpty() && (baseBrand == null || !selected.containsKey(baseBrand))) {
      result.append(base).append('\n');
    }
    result
        .append(storeContextFormatter.savedBranchesHeader())
        .append('\n')
        .append(String.join("\n", lines));
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
