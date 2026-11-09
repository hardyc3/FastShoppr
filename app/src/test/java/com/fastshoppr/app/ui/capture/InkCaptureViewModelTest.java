/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.InkData;
import com.fastshoppr.app.domain.model.InkPoint;
import com.fastshoppr.app.domain.model.InkStroke;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.InkRecognitionRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.navigation.NavArguments;
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
public final class InkCaptureViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Mock private CaptureRepository captureRepository;

  private final FakeInkRecognitionRepository inkRepository = new FakeInkRecognitionRepository();
  private final List<InkCaptureViewModel.InkUiEvent> events = new ArrayList<>();
  private final IdGenerator idGenerator = new FixedIdGenerator();
  private final Clock clock = new FixedClock(1000L);

  private InkCaptureViewModel viewModel;

  @Before
  public void setUp() {
    events.clear();
    inkRepository.reset();
    when(captureRepository.encodeRecognizedLines(any())).thenReturn("[\"lines\"]");
    viewModel = createViewModel();
    viewModel.getEvents().observeForever(event -> record(event.getContentIfNotHandled()));
  }

  private InkCaptureViewModel createViewModel() {
    return new InkCaptureViewModel(
        captureRepository,
        inkRepository,
        idGenerator,
        clock,
        new DirectExecutorService(),
        new SavedStateHandle(Map.of(NavArguments.LIST_ID, "list-1")));
  }

  private void record(InkCaptureViewModel.InkUiEvent event) {
    if (event != null) {
      events.add(event);
    }
  }

  private static InkData sampleInk() {
    return new InkData(
        100,
        200,
        List.of(
            new InkStroke(List.of(new InkPoint(10f, 20f, 1000L), new InkPoint(14f, 24f, 1100L)))));
  }

  @Test
  public void finishCreatesInkCaptureAndNavigatesToReview() {
    inkRepository.result = List.of(new RecognizedLine("milk", 0.95f));

    viewModel.finish(sampleInk());

    ArgumentCaptor<CaptureSession> captor = ArgumentCaptor.forClass(CaptureSession.class);
    verify(captureRepository).insert(captor.capture());
    CaptureSession session = captor.getValue();
    assertEquals("list-1", session.getListId());
    assertEquals(ItemSource.DIGITAL_INK, session.getSource());
    assertEquals("[\"lines\"]", session.getRecognizedLinesJson());
    assertEquals(null, session.getImageUri());
    InkCaptureViewModel.InkUiEvent.NavigateToReview event =
        findEvent(InkCaptureViewModel.InkUiEvent.NavigateToReview.class);
    assertNotNull(event);
    assertEquals("id-1", event.captureId());
    assertFalse(inkRepository.closed);
  }

  @Test
  public void finishWithoutStrokesRequiresWriting() {
    viewModel.finish(new InkData(100, 200, List.of()));

    assertNotNull(findEvent(InkCaptureViewModel.InkUiEvent.StrokesRequired.class));
    verify(captureRepository, never()).insert(any());
  }

  @Test
  public void finishWhileModelNotReadyReportsError() {
    inkRepository.ready = false;
    inkRepository.downloading = true;
    viewModel = createViewModel();
    viewModel.getEvents().observeForever(event -> record(event.getContentIfNotHandled()));

    viewModel.finish(sampleInk());

    InkCaptureViewModel.InkUiEvent.ShowError error =
        findEvent(InkCaptureViewModel.InkUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.INK_MODEL_NOT_READY, error.error());
    verify(captureRepository, never()).insert(any());
  }

  @Test
  public void emptyRecognitionDoesNotCreateCapture() {
    inkRepository.result = List.of();

    viewModel.finish(sampleInk());

    assertNotNull(findEvent(InkCaptureViewModel.InkUiEvent.EmptyRecognition.class));
    verify(captureRepository, never()).insert(any());
  }

  @Test
  public void recognitionFailureReportsError() {
    inkRepository.failure = new IllegalStateException("boom");

    viewModel.finish(sampleInk());

    InkCaptureViewModel.InkUiEvent.ShowError error =
        findEvent(InkCaptureViewModel.InkUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.RECOGNITION_FAILED, error.error());
    verify(captureRepository, never()).insert(any());
  }

  @Test
  public void modelDownloadFailureMarksStateFailed() {
    inkRepository.ready = false;
    inkRepository.failure = new IllegalStateException("offline");
    viewModel = createViewModel();

    assertEquals(
        InkCaptureViewModel.ModelState.FAILED, viewModel.getState().getValue().modelState());
  }

  @Test
  public void clearedViewModelClosesRecognizer() {
    viewModel.onCleared();

    assertTrue(inkRepository.closed);
  }

  private <T extends InkCaptureViewModel.InkUiEvent> T findEvent(Class<T> type) {
    for (InkCaptureViewModel.InkUiEvent event : events) {
      if (type.isInstance(event)) {
        return type.cast(event);
      }
    }
    return null;
  }

  private static final class FakeInkRecognitionRepository implements InkRecognitionRepository {

    boolean ready = true;
    boolean downloading;
    boolean closed;
    List<RecognizedLine> result = List.of();
    RuntimeException failure;

    void reset() {
      ready = true;
      downloading = false;
      closed = false;
      result = List.of();
      failure = null;
    }

    @Override
    public void prepareModel(ModelListener listener) {
      if (failure != null && !ready) {
        listener.onError(failure);
      } else if (!ready || downloading) {
        listener.onDownloading();
      } else {
        listener.onReady();
      }
    }

    @Override
    public CompletableFuture<List<RecognizedLine>> recognize(InkData ink) {
      CompletableFuture<List<RecognizedLine>> future = new CompletableFuture<>();
      if (failure != null) {
        future.completeExceptionally(failure);
      } else {
        future.complete(result);
      }
      return future;
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
