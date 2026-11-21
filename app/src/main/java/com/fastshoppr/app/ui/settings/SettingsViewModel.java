/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.concurrent.MainThreadExecutor;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorBlockedException;
import com.fastshoppr.app.domain.repository.LocationSource;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.usecase.DeleteAllDataUseCase;
import com.fastshoppr.app.domain.usecase.FindNearbyStoresUseCase;
import com.fastshoppr.app.domain.usecase.ForgetSelectedStoreUseCase;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.domain.usecase.SaveSelectedStoreUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

@HiltViewModel
public final class SettingsViewModel extends ViewModel {

  public enum AiStatus {
    CHECKING,
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
    QUOTA_EXCEEDED,
    DOWNLOAD_FAILED
  }

  public enum StoreMessage {
    ZIP_REQUIRED,
    MANUAL_NAME_REQUIRED,
    LOCATION_UNAVAILABLE,
    LOCATION_DENIED,
    SEARCH_FAILED,
    BLOCKED,
    NO_RESULTS,
    ZIP_SAVED,
    STORE_SAVED,
    STORE_REMOVED
  }

  /** Nearby candidates paired with the brand that was searched, so the picker stays correct. */
  public static final class StoreSearchResults {
    private final StoreBrand brand;
    private final List<NearbyStore> stores;

    StoreSearchResults(StoreBrand brand, List<NearbyStore> stores) {
      this.brand = brand;
      this.stores = stores;
    }

    public StoreBrand getBrand() {
      return brand;
    }

    public List<NearbyStore> getStores() {
      return stores;
    }
  }

  public static final class SettingsUiState {
    private final boolean aiCleanupEnabled;
    private final AiStatus aiStatus;
    private final long downloadedBytes;
    private final String postalCode;
    private final Map<StoreBrand, NearbyStore> selectedStores;
    private final boolean storeSearching;

    SettingsUiState(
        boolean aiCleanupEnabled,
        AiStatus aiStatus,
        long downloadedBytes,
        String postalCode,
        Map<StoreBrand, NearbyStore> selectedStores,
        boolean storeSearching) {
      this.aiCleanupEnabled = aiCleanupEnabled;
      this.aiStatus = aiStatus;
      this.downloadedBytes = downloadedBytes;
      this.postalCode = postalCode;
      this.selectedStores = selectedStores;
      this.storeSearching = storeSearching;
    }

    public boolean isAiCleanupEnabled() {
      return aiCleanupEnabled;
    }

    public AiStatus getAiStatus() {
      return aiStatus;
    }

    public long getDownloadedBytes() {
      return downloadedBytes;
    }

    public String getPostalCode() {
      return postalCode;
    }

    public Map<StoreBrand, NearbyStore> getSelectedStores() {
      return selectedStores;
    }

    public boolean isStoreSearching() {
      return storeSearching;
    }
  }

  private final SettingsRepository settingsRepository;
  private final ItemNameCleanerCoordinator cleanerCoordinator;
  private final DeleteAllDataUseCase deleteAllDataUseCase;
  private final MainThreadExecutor mainThread;
  private final LocationSource locationSource;
  private final FindNearbyStoresUseCase findNearbyStores;
  private final SaveSelectedStoreUseCase saveSelectedStore;
  private final ForgetSelectedStoreUseCase forgetSelectedStore;
  private final GetSelectedStoresUseCase getSelectedStores;

  private final MutableLiveData<SettingsUiState> state = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> deleteEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<StoreSearchResults>> nearbyStoresEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<StoreMessage>> storeMessageEvent =
      new MutableLiveData<>();

  private boolean aiCleanupEnabled;
  private AiStatus aiStatus = AiStatus.CHECKING;
  private long downloadedBytes;
  private String postalCode;
  private Map<StoreBrand, NearbyStore> selectedStores = Collections.emptyMap();
  private boolean storeSearching;
  private int generation;
  private int storeSearchGeneration;

  @Inject
  public SettingsViewModel(
      SettingsRepository settingsRepository,
      ItemNameCleanerCoordinator cleanerCoordinator,
      DeleteAllDataUseCase deleteAllDataUseCase,
      MainThreadExecutor mainThread,
      LocationSource locationSource,
      FindNearbyStoresUseCase findNearbyStores,
      SaveSelectedStoreUseCase saveSelectedStore,
      ForgetSelectedStoreUseCase forgetSelectedStore,
      GetSelectedStoresUseCase getSelectedStores) {
    this.settingsRepository = settingsRepository;
    this.cleanerCoordinator = cleanerCoordinator;
    this.deleteAllDataUseCase = deleteAllDataUseCase;
    this.mainThread = mainThread;
    this.locationSource = locationSource;
    this.findNearbyStores = findNearbyStores;
    this.saveSelectedStore = saveSelectedStore;
    this.forgetSelectedStore = forgetSelectedStore;
    this.getSelectedStores = getSelectedStores;
    this.aiCleanupEnabled = settingsRepository.isAiCleanupEnabled();
    refreshStoreState();
    state.setValue(currentState());
  }

  public LiveData<SettingsUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<Boolean>> getDeleteEvent() {
    return deleteEvent;
  }

  public LiveData<SingleEvent<StoreSearchResults>> getNearbyStoresEvent() {
    return nearbyStoresEvent;
  }

  public LiveData<SingleEvent<StoreMessage>> getStoreMessageEvent() {
    return storeMessageEvent;
  }

  /** Clears local data after the user confirms the destructive action. */
  public void deleteAllData() {
    deleteAllDataUseCase
        .execute()
        .whenComplete(
            (ignored, error) ->
                postToMain(
                    () -> {
                      deleteEvent.postValue(new SingleEvent<>(error == null));
                      if (error == null) {
                        refreshStoreState();
                        publish();
                      }
                    }));
  }

  public void load() {
    aiStatus = AiStatus.CHECKING;
    publish();
    final int requestGeneration = ++generation;
    cleanerCoordinator
        .availability()
        .whenComplete(
            (availability, error) ->
                postToMain(
                    () -> {
                      if (requestGeneration != generation) {
                        return;
                      }
                      aiStatus = mapAvailability(availability, error);
                      publish();
                    }));
  }

  public void setAiCleanupEnabled(boolean enabled) {
    aiCleanupEnabled = enabled;
    settingsRepository.setAiCleanupEnabled(enabled);
    publish();
  }

  /** Downloads the model only after a user action and reports progress. */
  public void downloadModel() {
    if (aiStatus != AiStatus.DOWNLOADABLE) {
      return;
    }
    aiStatus = AiStatus.DOWNLOADING;
    downloadedBytes = 0;
    publish();
    final int requestGeneration = ++generation;
    cleanerCoordinator
        .downloadModel(
            bytes ->
                postToMain(
                    () -> {
                      if (requestGeneration != generation) {
                        return;
                      }
                      downloadedBytes = bytes;
                      publish();
                    }))
        .whenComplete(
            (ignored, error) ->
                postToMain(
                    () -> {
                      if (requestGeneration != generation) {
                        return;
                      }
                      if (error != null) {
                        aiStatus = AiStatus.DOWNLOAD_FAILED;
                        publish();
                        return;
                      }
                      load();
                    }));
  }

  public void savePostalCode(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isEmpty()) {
      postStoreMessage(StoreMessage.ZIP_REQUIRED);
      return;
    }
    saveSelectedStore.savePostalCode(normalized);
    postalCode = normalized;
    publish();
    postStoreMessage(StoreMessage.ZIP_SAVED);
  }

  public void findNearbyUsingPostalCode(StoreBrand brand) {
    if (postalCode == null || postalCode.isBlank()) {
      postStoreMessage(StoreMessage.ZIP_REQUIRED);
      return;
    }
    search(brand, null, postalCode, ++storeSearchGeneration);
  }

  public void findNearbyUsingLocation(StoreBrand brand) {
    final int requestGeneration = ++storeSearchGeneration;
    storeSearching = true;
    publish();
    locationSource
        .lastKnownLocation()
        .whenComplete(
            (point, error) ->
                postToMain(
                    () -> {
                      if (requestGeneration != storeSearchGeneration) {
                        return;
                      }
                      if (error != null || point == null) {
                        storeSearching = false;
                        publish();
                        postStoreMessage(StoreMessage.LOCATION_UNAVAILABLE);
                        return;
                      }
                      search(brand, point, null, requestGeneration);
                    }));
  }

  public void locationPermissionDenied() {
    postStoreMessage(StoreMessage.LOCATION_DENIED);
  }

  public void saveStore(NearbyStore store) {
    saveSelectedStore.execute(store);
    refreshStoreState();
    publish();
    postStoreMessage(StoreMessage.STORE_SAVED);
  }

  public void saveManualStore(StoreBrand brand, String name, String address) {
    String trimmedName = name == null ? "" : name.trim();
    if (trimmedName.isEmpty()) {
      postStoreMessage(StoreMessage.MANUAL_NAME_REQUIRED);
      return;
    }
    String id = "manual-" + brand.name().toLowerCase(Locale.US) + "-" + trimmedName;
    NearbyStore store =
        new NearbyStore(
            id,
            brand,
            trimmedName,
            address,
            null,
            null,
            postalCode,
            null,
            null,
            NearbyStoreSource.MANUAL);
    saveStore(store);
  }

  public void forgetStore(StoreBrand brand) {
    forgetSelectedStore.execute(brand);
    refreshStoreState();
    publish();
    postStoreMessage(StoreMessage.STORE_REMOVED);
  }

  private void search(StoreBrand brand, GeoPoint origin, String zip, int requestGeneration) {
    storeSearching = true;
    publish();
    final CompletableFuture<List<NearbyStore>> searchResult;
    try {
      searchResult = findNearbyStores.execute(brand, origin, zip);
    } catch (IllegalArgumentException invalidRequest) {
      storeSearching = false;
      publish();
      postStoreMessage(StoreMessage.SEARCH_FAILED);
      return;
    }
    searchResult.whenComplete(
        (stores, error) ->
            postToMain(
                () -> {
                  if (requestGeneration != storeSearchGeneration) {
                    return;
                  }
                  storeSearching = false;
                  if (error != null) {
                    publish();
                    postStoreMessage(mapLookupError(error));
                    return;
                  }
                  publish();
                  if (stores == null || stores.isEmpty()) {
                    postStoreMessage(StoreMessage.NO_RESULTS);
                  } else {
                    nearbyStoresEvent.postValue(
                        new SingleEvent<>(new StoreSearchResults(brand, stores)));
                  }
                }));
  }

  private static StoreMessage mapLookupError(Throwable error) {
    return error instanceof StoreLocatorBlockedException
        ? StoreMessage.BLOCKED
        : StoreMessage.SEARCH_FAILED;
  }

  private void refreshStoreState() {
    postalCode = getSelectedStores.getPostalCode();
    selectedStores = getSelectedStores.execute();
  }

  private void postStoreMessage(StoreMessage message) {
    postToMain(() -> storeMessageEvent.postValue(new SingleEvent<>(message)));
  }

  private void postToMain(Runnable action) {
    mainThread.execute(action);
  }

  private AiStatus mapAvailability(AiAvailability availability, Throwable error) {
    if (error != null || availability == null) {
      return AiStatus.UNAVAILABLE;
    }
    if (availability instanceof AiAvailability.Available) {
      return AiStatus.AVAILABLE;
    }
    if (availability instanceof AiAvailability.Downloadable) {
      return AiStatus.DOWNLOADABLE;
    }
    if (availability instanceof AiAvailability.Downloading) {
      return AiStatus.DOWNLOADING;
    }
    if (availability instanceof AiAvailability.QuotaExceeded) {
      return AiStatus.QUOTA_EXCEEDED;
    }
    return AiStatus.UNAVAILABLE;
  }

  private void publish() {
    state.postValue(currentState());
  }

  private SettingsUiState currentState() {
    return new SettingsUiState(
        aiCleanupEnabled, aiStatus, downloadedBytes, postalCode, selectedStores, storeSearching);
  }

  @Override
  protected void onCleared() {
    generation++;
    storeSearchGeneration++;
  }
}
