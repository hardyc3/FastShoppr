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
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.InkData;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.InkRecognitionRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.usecase.DeleteAllDataUseCase;
import com.fastshoppr.app.domain.usecase.ForgetSelectedStoreUseCase;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
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
    STORE_REMOVED,
    STORE_CLEAR_FAILED
  }

  public enum InkModelDeleteStatus {
    IDLE,
    DELETING,
    DELETED,
    FAILED
  }

  private static final InkRecognitionRepository NO_OP_INK_REPOSITORY =
      new InkRecognitionRepository() {
        @Override
        public void prepareModel(ModelListener listener) {
          listener.onReady();
        }

        @Override
        public CompletableFuture<List<RecognizedLine>> recognize(InkData ink) {
          return CompletableFuture.completedFuture(List.of());
        }

        @Override
        public void close() {}
      };

  public static final class SettingsUiState {
    private final boolean aiCleanupEnabled;
    private final AiStatus aiStatus;
    private final long downloadedBytes;
    private final Map<StoreBrand, NearbyStore> selectedStores;
    private final InkModelDeleteStatus inkModelDeleteStatus;

    SettingsUiState(
        boolean aiCleanupEnabled,
        AiStatus aiStatus,
        long downloadedBytes,
        Map<StoreBrand, NearbyStore> selectedStores,
        InkModelDeleteStatus inkModelDeleteStatus) {
      this.aiCleanupEnabled = aiCleanupEnabled;
      this.aiStatus = aiStatus;
      this.downloadedBytes = downloadedBytes;
      this.selectedStores = selectedStores;
      this.inkModelDeleteStatus = inkModelDeleteStatus;
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

    public Map<StoreBrand, NearbyStore> getSelectedStores() {
      return selectedStores;
    }

    public InkModelDeleteStatus getInkModelDeleteStatus() {
      return inkModelDeleteStatus;
    }
  }

  private final SettingsRepository settingsRepository;
  private final ItemNameCleanerCoordinator cleanerCoordinator;
  private final DeleteAllDataUseCase deleteAllDataUseCase;
  private final ForgetSelectedStoreUseCase forgetSelectedStore;
  private final GetSelectedStoresUseCase getSelectedStores;
  private final InkRecognitionRepository inkRecognitionRepository;

  private final MutableLiveData<SettingsUiState> state = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> deleteEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<StoreMessage>> storeMessageEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> inkModelDeleteEvent = new MutableLiveData<>();

  private boolean aiCleanupEnabled;
  private AiStatus aiStatus = AiStatus.CHECKING;
  private long downloadedBytes;
  private Map<StoreBrand, NearbyStore> selectedStores = Collections.emptyMap();
  private InkModelDeleteStatus inkModelDeleteStatus = InkModelDeleteStatus.IDLE;
  private int generation;

  @Inject
  public SettingsViewModel(
      SettingsRepository settingsRepository,
      ItemNameCleanerCoordinator cleanerCoordinator,
      DeleteAllDataUseCase deleteAllDataUseCase,
      ForgetSelectedStoreUseCase forgetSelectedStore,
      GetSelectedStoresUseCase getSelectedStores,
      InkRecognitionRepository inkRecognitionRepository) {
    this.settingsRepository = settingsRepository;
    this.cleanerCoordinator = cleanerCoordinator;
    this.deleteAllDataUseCase = deleteAllDataUseCase;
    this.forgetSelectedStore = forgetSelectedStore;
    this.getSelectedStores = getSelectedStores;
    this.inkRecognitionRepository = inkRecognitionRepository;
    this.aiCleanupEnabled = settingsRepository.isAiCleanupEnabled();
    refreshStoreState();
    state.setValue(currentState());
  }

  /** Kept for callers that do not manage ink models; Hilt uses the six-argument constructor. */
  public SettingsViewModel(
      SettingsRepository settingsRepository,
      ItemNameCleanerCoordinator cleanerCoordinator,
      DeleteAllDataUseCase deleteAllDataUseCase,
      ForgetSelectedStoreUseCase forgetSelectedStore,
      GetSelectedStoresUseCase getSelectedStores) {
    this(
        settingsRepository,
        cleanerCoordinator,
        deleteAllDataUseCase,
        forgetSelectedStore,
        getSelectedStores,
        NO_OP_INK_REPOSITORY);
  }

  public LiveData<SettingsUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<Boolean>> getDeleteEvent() {
    return deleteEvent;
  }

  public LiveData<SingleEvent<StoreMessage>> getStoreMessageEvent() {
    return storeMessageEvent;
  }

  public LiveData<SingleEvent<Boolean>> getInkModelDeleteEvent() {
    return inkModelDeleteEvent;
  }

  /** Deletes the downloaded handwriting model asynchronously; other settings stay usable. */
  public void deleteInkModel() {
    if (inkModelDeleteStatus == InkModelDeleteStatus.DELETING) {
      return;
    }
    inkModelDeleteStatus = InkModelDeleteStatus.DELETING;
    publish();
    try {
      inkRecognitionRepository
          .deleteDownloadedModel()
          .whenComplete(
              (ignored, error) -> {
                inkModelDeleteStatus =
                    error == null ? InkModelDeleteStatus.DELETED : InkModelDeleteStatus.FAILED;
                publish();
                inkModelDeleteEvent.postValue(new SingleEvent<>(error == null));
              });
    } catch (RuntimeException error) {
      inkModelDeleteStatus = InkModelDeleteStatus.FAILED;
      publish();
      inkModelDeleteEvent.postValue(new SingleEvent<>(false));
    }
  }

  /** Refreshes saved branches after returning from the branch-picker screen. */
  public void refresh() {
    refreshStoreState();
    publish();
  }

  /** Clears local data after the user confirms the destructive action. */
  public void deleteAllData() {
    deleteAllDataUseCase
        .execute()
        .whenComplete(
            (ignored, error) -> {
              deleteEvent.postValue(new SingleEvent<>(error == null));
              if (error == null) {
                refreshStoreState();
                publish();
              }
            });
  }

  public void load() {
    aiStatus = AiStatus.CHECKING;
    publish();
    final int requestGeneration = ++generation;
    cleanerCoordinator
        .availability()
        .whenComplete(
            (availability, error) -> {
              if (requestGeneration != generation) {
                return;
              }
              aiStatus = mapAvailability(availability, error);
              publish();
            });
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
            bytes -> {
              if (requestGeneration != generation) {
                return;
              }
              downloadedBytes = bytes;
              publish();
            })
        .whenComplete(
            (ignored, error) -> {
              if (requestGeneration != generation) {
                return;
              }
              if (error != null) {
                aiStatus = AiStatus.DOWNLOAD_FAILED;
                publish();
                return;
              }
              load();
            });
  }

  public void forgetStore(StoreBrand brand) {
    forgetSelectedStore
        .execute(brand)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                storeMessageEvent.postValue(new SingleEvent<>(StoreMessage.STORE_CLEAR_FAILED));
                return;
              }
              refreshStoreState();
              publish();
              storeMessageEvent.postValue(new SingleEvent<>(StoreMessage.STORE_REMOVED));
            });
  }

  private void refreshStoreState() {
    selectedStores = getSelectedStores.execute();
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
        aiCleanupEnabled, aiStatus, downloadedBytes, selectedStores, inkModelDeleteStatus);
  }

  @Override
  protected void onCleared() {
    generation++;
  }
}
