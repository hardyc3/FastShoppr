/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ImageTextRecognitionRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.navigation.NavArguments;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class CameraCaptureViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Mock private CaptureRepository captureRepository;
  @Mock private ImageTextRecognitionRepository textRecognitionRepository;
  @Mock private CaptureImageStore imageStore;

  private final List<CameraCaptureViewModel.CameraUiEvent> events = new ArrayList<>();
  private final IdGenerator idGenerator = new FixedIdGenerator();
  private final Clock clock = new FixedClock(1000L);

  private CameraCaptureViewModel viewModel;

  @Before
  public void setUp() throws Exception {
    events.clear();
    when(captureRepository.encodeRecognizedLines(any())).thenReturn("[\"lines\"]");
    when(imageStore.copyToCache(any())).thenReturn(new File("cache", "picked.jpg"));
    viewModel = createViewModel();
    viewModel.getEvents().observeForever(event -> record(event.getContentIfNotHandled()));
  }

  private CameraCaptureViewModel createViewModel() {
    return new CameraCaptureViewModel(
        captureRepository,
        textRecognitionRepository,
        imageStore,
        idGenerator,
        clock,
        new DirectExecutorService(),
        new SavedStateHandle(Map.of(NavArguments.LIST_ID, "list-1")));
  }

  private void record(CameraCaptureViewModel.CameraUiEvent event) {
    if (event != null) {
      events.add(event);
    }
  }

  private void recognitionReturns(CompletableFuture<List<RecognizedLine>> future) {
    when(textRecognitionRepository.recognize(anyString())).thenReturn(future);
  }

  @Test
  public void capturedPhotoCreatesCameraCaptureAndNavigates() {
    recognitionReturns(
        CompletableFuture.completedFuture(List.of(new RecognizedLine("milk", 0.9f))));

    viewModel.onImageCaptured("/cache/captures/photo.jpg");

    ArgumentCaptor<CaptureSession> captor = ArgumentCaptor.forClass(CaptureSession.class);
    verify(captureRepository).insert(captor.capture());
    CaptureSession session = captor.getValue();
    assertEquals(ItemSource.CAMERA_OCR, session.getSource());
    assertEquals("/cache/captures/photo.jpg", session.getImageUri());
    CameraCaptureViewModel.CameraUiEvent.NavigateToReview event =
        findEvent(CameraCaptureViewModel.CameraUiEvent.NavigateToReview.class);
    assertNotNull(event);
    assertEquals("id-1", event.captureId());
  }

  @Test
  public void pickedImageCreatesGalleryCaptureWithCachedCopy() {
    recognitionReturns(
        CompletableFuture.completedFuture(List.of(new RecognizedLine("bread", 0.8f))));

    viewModel.importImage(null);

    ArgumentCaptor<CaptureSession> captor = ArgumentCaptor.forClass(CaptureSession.class);
    verify(captureRepository).insert(captor.capture());
    CaptureSession session = captor.getValue();
    assertEquals(ItemSource.GALLERY_OCR, session.getSource());
    assertTrue(session.getImageUri().endsWith("picked.jpg"));
    assertNotNull(findEvent(CameraCaptureViewModel.CameraUiEvent.NavigateToReview.class));
  }

  @Test
  public void emptyRecognitionReportsOptionsWithoutSession() {
    recognitionReturns(CompletableFuture.completedFuture(List.of()));

    viewModel.onImageCaptured("/cache/captures/photo.jpg");

    assertNotNull(findEvent(CameraCaptureViewModel.CameraUiEvent.EmptyRecognition.class));
    verify(captureRepository, never()).insert(any());
  }

  @Test
  public void recognitionFailureReportsError() {
    recognitionReturns(failedFuture(new IOExceptionStub()));

    viewModel.onImageCaptured("/cache/captures/photo.jpg");

    CameraCaptureViewModel.CameraUiEvent.ShowError error =
        findEvent(CameraCaptureViewModel.CameraUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.RECOGNITION_FAILED, error.error());
    verify(captureRepository, never()).insert(any());
  }

  @Test
  public void retakeDeletesPendingImageWithoutSession() {
    recognitionReturns(CompletableFuture.completedFuture(List.of()));

    viewModel.onImageCaptured("/cache/captures/photo.jpg");
    viewModel.retake();

    verify(imageStore).delete("/cache/captures/photo.jpg");
    assertTrue(!viewModel.getState().getValue().canRetake());
  }

  @Test
  public void clearedViewModelDeletesImageWithoutSession() {
    recognitionReturns(CompletableFuture.completedFuture(List.of()));

    viewModel.onImageCaptured("/cache/captures/photo.jpg");
    viewModel.onCleared();

    verify(imageStore).delete("/cache/captures/photo.jpg");
  }

  @Test
  public void clearedViewModelKeepsImageOwnedByCreatedSession() {
    recognitionReturns(
        CompletableFuture.completedFuture(List.of(new RecognizedLine("milk", 0.9f))));

    viewModel.onImageCaptured("/cache/captures/photo.jpg");
    viewModel.onCleared();

    verify(captureRepository).insert(any(CaptureSession.class));
    verify(imageStore, never()).delete(anyString());
  }

  private <T extends CameraCaptureViewModel.CameraUiEvent> T findEvent(Class<T> type) {
    for (CameraCaptureViewModel.CameraUiEvent event : events) {
      if (type.isInstance(event)) {
        return type.cast(event);
      }
    }
    return null;
  }

  private static CompletableFuture<List<RecognizedLine>> failedFuture(Throwable error) {
    CompletableFuture<List<RecognizedLine>> future = new CompletableFuture<>();
    future.completeExceptionally(error);
    return future;
  }

  private static final class IOExceptionStub extends RuntimeException {
    IOExceptionStub() {
      super("read failed");
    }
  }
}
