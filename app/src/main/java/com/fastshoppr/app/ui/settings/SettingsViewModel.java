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
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.usecase.DeleteAllDataUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import dagger.hilt.android.lifecycle.HiltViewModel;
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

  public static final class SettingsUiState {
    private final boolean aiCleanupEnabled;
    private final AiStatus aiStatus;
    private final long downloadedBytes;

    SettingsUiState(boolean aiCleanupEnabled, AiStatus aiStatus, long downloadedBytes) {
      this.aiCleanupEnabled = aiCleanupEnabled;
      this.aiStatus = aiStatus;
      this.downloadedBytes = downloadedBytes;
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
  }

  private final SettingsRepository settingsRepository;
  private final ItemNameCleanerCoordinator cleanerCoordinator;
  private final DeleteAllDataUseCase deleteAllDataUseCase;
  private final MainThreadExecutor mainThread;
  private final MutableLiveData<SettingsUiState> state = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> deleteEvent = new MutableLiveData<>();

  private boolean aiCleanupEnabled;
  private AiStatus aiStatus = AiStatus.CHECKING;
  private long downloadedBytes;
  private int generation;

  @Inject
  public SettingsViewModel(
      SettingsRepository settingsRepository,
      ItemNameCleanerCoordinator cleanerCoordinator,
      DeleteAllDataUseCase deleteAllDataUseCase,
      MainThreadExecutor mainThread) {
    this.settingsRepository = settingsRepository;
    this.cleanerCoordinator = cleanerCoordinator;
    this.deleteAllDataUseCase = deleteAllDataUseCase;
    this.mainThread = mainThread;
    this.aiCleanupEnabled = settingsRepository.isAiCleanupEnabled();
    state.setValue(currentState());
  }

  public LiveData<SettingsUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<Boolean>> getDeleteEvent() {
    return deleteEvent;
  }

  /** Clears local data after the user confirms the destructive action. */
  public void deleteAllData() {
    deleteAllDataUseCase
        .execute()
        .whenComplete(
            (ignored, error) ->
                postToMain(() -> deleteEvent.postValue(new SingleEvent<>(error == null))));
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
    return new SettingsUiState(aiCleanupEnabled, aiStatus, downloadedBytes);
  }

  @Override
  protected void onCleared() {
    generation++;
  }
}
