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
import com.fastshoppr.app.domain.usecase.CreateTypedCaptureUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Objects;
import javax.inject.Inject;

@HiltViewModel
public final class TypedCaptureViewModel extends ViewModel {

  public record TypedCaptureUiState(boolean saving) {}

  public sealed interface TypedCaptureUiEvent
      permits TypedCaptureUiEvent.NavigateToReview, TypedCaptureUiEvent.ShowError {

    record NavigateToReview(String captureId) implements TypedCaptureUiEvent {}

    record ShowError(UiError error) implements TypedCaptureUiEvent {}
  }

  private final CreateTypedCaptureUseCase createTypedCaptureUseCase;
  private final String listId;

  private final MutableLiveData<TypedCaptureUiState> state =
      new MutableLiveData<>(new TypedCaptureUiState(false));
  private final MutableLiveData<SingleEvent<TypedCaptureUiEvent>> events = new MutableLiveData<>();

  @Inject
  public TypedCaptureViewModel(
      CreateTypedCaptureUseCase createTypedCaptureUseCase, SavedStateHandle savedStateHandle) {
    this.createTypedCaptureUseCase = createTypedCaptureUseCase;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
  }

  public LiveData<TypedCaptureUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<TypedCaptureUiEvent>> getEvents() {
    return events;
  }

  /** Creates the real capture session before asking the UI to open review. */
  public void capture(String typedText) {
    if (typedText == null || typedText.trim().isEmpty()) {
      postEvent(new TypedCaptureUiEvent.ShowError(UiError.RAW_TEXT_REQUIRED));
      return;
    }
    if (Boolean.TRUE.equals(state.getValue().saving())) {
      return;
    }
    state.setValue(new TypedCaptureUiState(true));
    createTypedCaptureUseCase
        .create(listId, typedText)
        .whenComplete(
            (captureId, error) -> {
              state.postValue(new TypedCaptureUiState(false));
              if (error != null || captureId == null) {
                postEvent(new TypedCaptureUiEvent.ShowError(UiError.SAVE_FAILED));
                return;
              }
              postEvent(new TypedCaptureUiEvent.NavigateToReview(captureId));
            });
  }

  private void postEvent(TypedCaptureUiEvent event) {
    events.postValue(new SingleEvent<>(event));
  }
}
