/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.InkData;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.InkRecognitionRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class InkCaptureViewModel extends ViewModel {

  public enum ModelState {
    CHECKING,
    DOWNLOADING,
    READY,
    FAILED
  }

  public record InkCaptureUiState(ModelState modelState, boolean recognizing) {

    public static InkCaptureUiState checking() {
      return new InkCaptureUiState(ModelState.CHECKING, false);
    }
  }

  public sealed interface InkUiEvent
      permits InkUiEvent.NavigateToReview,
          InkUiEvent.ShowError,
          InkUiEvent.ShowMessage,
          InkUiEvent.EmptyRecognition,
          InkUiEvent.StrokesRequired {

    record NavigateToReview(String captureId) implements InkUiEvent {}

    record ShowError(UiError error) implements InkUiEvent {}

    record ShowMessage(UiMessage message) implements InkUiEvent {}

    record EmptyRecognition() implements InkUiEvent {}

    record StrokesRequired() implements InkUiEvent {}
  }

  private final CaptureRepository captureRepository;
  private final InkRecognitionRepository inkRecognitionRepository;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final ExecutorService executor;
  private final String listId;

  private final MutableLiveData<InkCaptureUiState> state =
      new MutableLiveData<>(InkCaptureUiState.checking());
  private final MutableLiveData<SingleEvent<InkUiEvent>> events = new MutableLiveData<>();

  @Inject
  public InkCaptureViewModel(
      CaptureRepository captureRepository,
      InkRecognitionRepository inkRecognitionRepository,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor,
      SavedStateHandle savedStateHandle) {
    this.captureRepository = captureRepository;
    this.inkRecognitionRepository = inkRecognitionRepository;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.executor = executor;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
    prepareModel();
  }

  public LiveData<InkCaptureUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<InkUiEvent>> getEvents() {
    return events;
  }

  private void prepareModel() {
    inkRecognitionRepository.prepareModel(
        new InkRecognitionRepository.ModelListener() {
          @Override
          public void onDownloading() {
            state.postValue(new InkCaptureUiState(ModelState.DOWNLOADING, isRecognizing()));
          }

          @Override
          public void onReady() {
            state.postValue(new InkCaptureUiState(ModelState.READY, isRecognizing()));
          }

          @Override
          public void onError(Exception error) {
            state.postValue(new InkCaptureUiState(ModelState.FAILED, isRecognizing()));
          }
        });
  }

  public void finish(InkData ink) {
    if (ink == null || ink.isEmpty()) {
      postEvent(new InkUiEvent.StrokesRequired());
      return;
    }
    InkCaptureUiState current = state.getValue();
    if (current == null || current.modelState() != ModelState.READY) {
      postEvent(new InkUiEvent.ShowError(UiError.INK_MODEL_NOT_READY));
      return;
    }
    state.postValue(new InkCaptureUiState(ModelState.READY, true));
    inkRecognitionRepository
        .recognize(ink)
        .whenComplete(
            (lines, error) -> {
              if (error != null) {
                state.postValue(new InkCaptureUiState(ModelState.READY, false));
                postEvent(new InkUiEvent.ShowError(UiError.RECOGNITION_FAILED));
                return;
              }
              if (lines == null || lines.isEmpty()) {
                state.postValue(new InkCaptureUiState(ModelState.READY, false));
                postEvent(new InkUiEvent.EmptyRecognition());
                return;
              }
              createSession(lines);
            });
  }

  private void createSession(List<RecognizedLine> lines) {
    String json = captureRepository.encodeRecognizedLines(lines);
    CompletableFuture.supplyAsync(
            () -> {
              CaptureSession session =
                  new CaptureSession(
                      idGenerator.newId(),
                      listId,
                      ItemSource.DIGITAL_INK,
                      null,
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
              state.postValue(new InkCaptureUiState(ModelState.READY, false));
              if (error != null) {
                postEvent(new InkUiEvent.ShowError(UiError.SAVE_FAILED));
                return;
              }
              postEvent(new InkUiEvent.NavigateToReview(captureId));
            });
  }

  private boolean isRecognizing() {
    InkCaptureUiState current = state.getValue();
    return current != null && current.recognizing();
  }

  private void postEvent(InkUiEvent event) {
    events.postValue(new SingleEvent<>(event));
  }

  @Override
  protected void onCleared() {
    inkRecognitionRepository.close();
  }
}
