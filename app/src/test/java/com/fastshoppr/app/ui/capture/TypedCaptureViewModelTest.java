/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.data.capture.CapturedLinesCodec;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.usecase.CreateTypedCaptureUseCase;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class TypedCaptureViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Mock private CaptureRepository captureRepository;

  private final CapturedLinesCodec codec = new CapturedLinesCodec(new Gson());
  private final List<TypedCaptureViewModel.TypedCaptureUiEvent> events = new ArrayList<>();
  private final FixedIdGenerator idGenerator = new FixedIdGenerator();
  private final FixedClock clock = new FixedClock(1000L);

  private TypedCaptureViewModel viewModel;

  @Before
  public void setUp() {
    events.clear();
    lenient()
        .when(captureRepository.encodeDraft(anyList()))
        .thenAnswer(call -> codec.encode(call.getArgument(0)));
    viewModel = createViewModel();
    viewModel.getEvents().observeForever(event -> record(event.getContentIfNotHandled()));
  }

  private TypedCaptureViewModel createViewModel() {
    CreateTypedCaptureUseCase useCase =
        new CreateTypedCaptureUseCase(
            captureRepository, idGenerator, clock, new DirectExecutorService());
    return new TypedCaptureViewModel(
        useCase, new SavedStateHandle(Map.of(NavArguments.LIST_ID, "list-1")));
  }

  private void record(TypedCaptureViewModel.TypedCaptureUiEvent event) {
    if (event != null) {
      events.add(event);
    }
  }

  @Test
  public void typedTextCreatesCaptureWithRealLinesAndNavigatesToReview() {
    viewModel.capture("  milk\n eggs  ");

    ArgumentCaptor<CaptureSession> captor = ArgumentCaptor.forClass(CaptureSession.class);
    verify(captureRepository).insert(captor.capture());
    CaptureSession session = captor.getValue();
    assertEquals("id-1", session.getId());
    assertEquals("list-1", session.getListId());
    assertEquals(ItemSource.TYPED, session.getSource());
    assertNull(session.getImageUri());
    assertEquals(1000L, session.getCreatedAt());
    assertNull(session.getCompletedAt());
    assertNull(session.getDeletedAt());

    List<CapturedLine> lines = codec.decode(session.getRecognizedLinesJson());
    assertEquals(2, lines.size());
    assertEquals("milk", lines.get(0).getText());
    assertEquals("eggs", lines.get(1).getText());
    assertTrue(lines.get(0).isSelected());
    assertTrue(lines.get(0).isManual());
    assertNull(lines.get(0).getDisplayName());
    assertNull(lines.get(0).getStoreId());
    assertNull(lines.get(0).getTargetDate());

    TypedCaptureViewModel.TypedCaptureUiEvent.NavigateToReview navigation =
        findEvent(TypedCaptureViewModel.TypedCaptureUiEvent.NavigateToReview.class);
    assertNotNull(navigation);
    assertEquals("id-1", navigation.captureId());
  }

  @Test
  public void blankTypedTextIsRejectedWithoutCreatingSession() {
    viewModel.capture("   ");

    TypedCaptureViewModel.TypedCaptureUiEvent.ShowError error =
        findEvent(TypedCaptureViewModel.TypedCaptureUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.RAW_TEXT_REQUIRED, error.error());
    verify(captureRepository, never()).insert(any(CaptureSession.class));
  }

  private <T extends TypedCaptureViewModel.TypedCaptureUiEvent> T findEvent(Class<T> type) {
    for (TypedCaptureViewModel.TypedCaptureUiEvent event : events) {
      if (type.isInstance(event)) {
        return type.cast(event);
      }
    }
    return null;
  }
}
