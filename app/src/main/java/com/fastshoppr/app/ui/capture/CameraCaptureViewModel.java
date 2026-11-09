/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ImageTextRecognitionRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class CameraCaptureViewModel extends ViewModel {

  public record CameraCaptureUiState(boolean recognizing, boolean canRetake) {}

  public sealed interface CameraUiEvent
      permits CameraUiEvent.NavigateToReview,
          CameraUiEvent.ShowError,
          CameraUiEvent.EmptyRecognition {

    record NavigateToReview(String captureId) implements CameraUiEvent {}

    record ShowError(UiError error) implements CameraUiEvent {}

    record EmptyRecognition() implements CameraUiEvent {}
  }

  private final CaptureRepository captureRepository;
  private final ImageTextRecognitionRepository textRecognitionRepository;
  private final CaptureImageStore imageStore;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final ExecutorService executor;
  private final String listId;

  private final MutableLiveData<CameraCaptureUiState> state =
      new MutableLiveData<>(new CameraCaptureUiState(false, false));
  private final MutableLiveData<SingleEvent<CameraUiEvent>> events = new MutableLiveData<>();

  private String pendingImagePath;
  private ItemSource pendingSource;
  private boolean sessionCreated;
  private long recognitionGeneration;

  @Inject
  public CameraCaptureViewModel(
      CaptureRepository captureRepository,
      ImageTextRecognitionRepository textRecognitionRepository,
      CaptureImageStore imageStore,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor,
      SavedStateHandle savedStateHandle) {
    this.captureRepository = captureRepository;
    this.textRecognitionRepository = textRecognitionRepository;
    this.imageStore = imageStore;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.executor = executor;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
  }

  public LiveData<CameraCaptureUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<CameraUiEvent>> getEvents() {
    return events;
  }

  /** Called when CameraX saved a captured still to the given app-private path. */
  public void onImageCaptured(String path) {
    if (path == null || path.isBlank()) {
      postEvent(new CameraUiEvent.ShowError(UiError.RECOGNITION_FAILED));
      return;
    }
    replacePendingImage();
    pendingImagePath = path;
    pendingSource = ItemSource.CAMERA_OCR;
    recognizePending();
  }

  /** Copies a picked image into cache and runs the same recognition pipeline. */
  public void importImage(Uri source) {
    long generation = beginRecognition();
    CompletableFuture.supplyAsync(
            () -> {
              try {
                return imageStore.copyToCache(source).getAbsolutePath();
              } catch (IOException exception) {
                throw new IllegalStateException(exception);
              }
            },
            executor)
        .whenComplete(
            (path, error) -> {
              if (generation != recognitionGeneration) {
                if (path != null) {
                  imageStore.delete(path);
                }
                return;
              }
              if (error != null) {
                state.postValue(new CameraCaptureUiState(false, canRetake()));
                postEvent(new CameraUiEvent.ShowError(UiError.RECOGNITION_FAILED));
                return;
              }
              replacePendingImage();
              pendingImagePath = path;
              pendingSource = ItemSource.GALLERY_OCR;
              runRecognition(path, generation);
            });
  }

  /** Deletes the pending image and returns to the preview. */
  public void retake() {
    recognitionGeneration++;
    replacePendingImage();
    state.postValue(new CameraCaptureUiState(false, false));
  }

  private void recognizePending() {
    long generation = beginRecognition();
    runRecognition(pendingImagePath, generation);
  }

  private void runRecognition(String path, long generation) {
    textRecognitionRepository
        .recognize(path)
        .whenComplete(
            (lines, error) -> {
              if (generation != recognitionGeneration) {
                return;
              }
              if (error != null) {
                state.postValue(new CameraCaptureUiState(false, canRetake()));
                postEvent(new CameraUiEvent.ShowError(UiError.RECOGNITION_FAILED));
                return;
              }
              if (lines == null || lines.isEmpty()) {
                state.postValue(new CameraCaptureUiState(false, canRetake()));
                postEvent(new CameraUiEvent.EmptyRecognition());
                return;
              }
              createSession(lines, generation);
            });
  }

  private long beginRecognition() {
    recognitionGeneration++;
    state.postValue(new CameraCaptureUiState(true, canRetake()));
    return recognitionGeneration;
  }

  private void createSession(List<RecognizedLine> lines, long generation) {
    String imagePath = pendingImagePath;
    ItemSource source = pendingSource == null ? ItemSource.CAMERA_OCR : pendingSource;
    String json = captureRepository.encodeRecognizedLines(lines);
    CompletableFuture.supplyAsync(
            () -> {
              CaptureSession session =
                  new CaptureSession(
                      idGenerator.newId(),
                      listId,
                      source,
                      imagePath,
                      json,
                      clock.nowMillis(),
                      null,
                      null);
              captureRepository.insert(session);
              return session.getId();
            },
            executor)
        .whenComplete(
            (captureId, error) -> {
              if (generation != recognitionGeneration) {
                return;
              }
              if (error != null) {
                replacePendingImage();
                state.postValue(new CameraCaptureUiState(false, false));
                postEvent(new CameraUiEvent.ShowError(UiError.SAVE_FAILED));
                return;
              }
              sessionCreated = true;
              state.postValue(new CameraCaptureUiState(false, false));
              postEvent(new CameraUiEvent.NavigateToReview(captureId));
            });
  }

  private void replacePendingImage() {
    if (pendingImagePath != null && !sessionCreated) {
      imageStore.delete(pendingImagePath);
    }
    pendingImagePath = null;
    pendingSource = null;
  }

  private boolean canRetake() {
    return pendingImagePath != null && pendingSource == ItemSource.CAMERA_OCR && !sessionCreated;
  }

  private void postEvent(CameraUiEvent event) {
    events.postValue(new SingleEvent<>(event));
  }

  @Override
  protected void onCleared() {
    textRecognitionRepository.close();
    if (!sessionCreated) {
      replacePendingImage();
    }
  }
}
