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
import com.fastshoppr.app.domain.model.PermissionAction;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.port.PermissionRequester;
import com.fastshoppr.app.domain.port.PermissionStatus;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ImageTextRecognitionRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.RequestPermissionUseCase;
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

  private static final PlatformPermission PERMISSION = PlatformPermission.CAMERA;
  private static final String KEY_PERMISSION_PREVIOUSLY_REQUESTED = "cameraPermissionRequested";
  private static final String KEY_RATIONALE_ACKNOWLEDGED = "cameraRationaleAcknowledged";

  public record CameraCaptureUiState(boolean recognizing, boolean canRetake, boolean granted) {}

  public sealed interface CameraUiEvent
      permits CameraUiEvent.NavigateToReview,
          CameraUiEvent.ShowError,
          CameraUiEvent.EmptyRecognition,
          CameraUiEvent.PermissionActionEvent {

    record NavigateToReview(String captureId) implements CameraUiEvent {}

    record ShowError(UiError error) implements CameraUiEvent {}

    record EmptyRecognition() implements CameraUiEvent {}

    record PermissionActionEvent(PermissionAction action) implements CameraUiEvent {}
  }

  private final CaptureRepository captureRepository;
  private final ImageTextRecognitionRepository textRecognitionRepository;
  private final CaptureImageStore imageStore;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final ExecutorService executor;
  private final PermissionStatus permissionStatus;
  private final PermissionRequester permissionRequester;
  private final RequestPermissionUseCase permissionResolver;
  private final SavedStateHandle savedStateHandle;
  private final String listId;

  private final MutableLiveData<CameraCaptureUiState> state =
      new MutableLiveData<>(new CameraCaptureUiState(false, false, false));
  private final MutableLiveData<SingleEvent<CameraUiEvent>> events = new MutableLiveData<>();

  private String pendingImagePath;
  private ItemSource pendingSource;
  private boolean sessionCreated;
  private long recognitionGeneration;
  private boolean permissionPreviouslyRequested;
  private boolean rationaleAcknowledged;
  private boolean lastPermissionGranted;
  private boolean lastShouldShowRationale;

  @Inject
  public CameraCaptureViewModel(
      CaptureRepository captureRepository,
      ImageTextRecognitionRepository textRecognitionRepository,
      CaptureImageStore imageStore,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor,
      PermissionStatus permissionStatus,
      PermissionRequester permissionRequester,
      RequestPermissionUseCase permissionResolver,
      SavedStateHandle savedStateHandle) {
    this.captureRepository = captureRepository;
    this.textRecognitionRepository = textRecognitionRepository;
    this.imageStore = imageStore;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.executor = executor;
    this.permissionStatus = permissionStatus;
    this.permissionRequester = permissionRequester;
    this.permissionResolver = permissionResolver;
    this.savedStateHandle = savedStateHandle;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
    permissionPreviouslyRequested =
        Boolean.TRUE.equals(savedStateHandle.get(KEY_PERMISSION_PREVIOUSLY_REQUESTED));
    rationaleAcknowledged = Boolean.TRUE.equals(savedStateHandle.get(KEY_RATIONALE_ACKNOWLEDGED));
    state.setValue(new CameraCaptureUiState(false, false, permissionStatus.isGranted(PERMISSION)));
  }

  public LiveData<CameraCaptureUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<CameraUiEvent>> getEvents() {
    return events;
  }

  /** Checks camera availability through the domain port when the screen starts. */
  public void onScreenStarted() {
    boolean granted = permissionStatus.isGranted(PERMISSION);
    publishGranted(granted);
    if (granted) {
      checkPermission(granted, false);
    }
  }

  /** Reads permission state through the domain port and performs the decided action. */
  public void checkPermission() {
    checkPermission(
        permissionStatus.isGranted(PERMISSION), permissionStatus.shouldShowRationale(PERMISSION));
  }

  private void checkPermission(boolean granted, boolean shouldShowRationale) {
    lastPermissionGranted = granted;
    lastShouldShowRationale = shouldShowRationale;
    publishGranted(granted);
    boolean permanentlyDenied = !granted && permissionPreviouslyRequested && !shouldShowRationale;
    boolean rationaleDone = rationaleAcknowledged;
    PermissionAction action = permissionResolver.resolve(granted, rationaleDone, permanentlyDenied);
    if (action == PermissionAction.REQUEST) {
      permissionRequester.request(PERMISSION, this::permissionResult);
      return;
    }
    postEvent(new CameraUiEvent.PermissionActionEvent(action));
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

  private void publishGranted(boolean granted) {
    CameraCaptureUiState current = state.getValue();
    boolean recognizing = current != null && current.recognizing();
    boolean canRetake = current != null && current.canRetake();
    state.setValue(new CameraCaptureUiState(recognizing, canRetake, granted));
  }

  private void postState(boolean recognizing, boolean canRetake) {
    CameraCaptureUiState current = state.getValue();
    boolean granted = current != null && current.granted();
    state.postValue(new CameraCaptureUiState(recognizing, canRetake, granted));
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
                postState(false, canRetake());
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
    postState(false, false);
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
                postState(false, canRetake());
                postEvent(new CameraUiEvent.ShowError(UiError.RECOGNITION_FAILED));
                return;
              }
              if (lines == null || lines.isEmpty()) {
                postState(false, canRetake());
                postEvent(new CameraUiEvent.EmptyRecognition());
                return;
              }
              createSession(lines, generation);
            });
  }

  private long beginRecognition() {
    recognitionGeneration++;
    postState(true, canRetake());
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
                postState(false, false);
                postEvent(new CameraUiEvent.ShowError(UiError.SAVE_FAILED));
                return;
              }
              sessionCreated = true;
              postState(false, false);
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
