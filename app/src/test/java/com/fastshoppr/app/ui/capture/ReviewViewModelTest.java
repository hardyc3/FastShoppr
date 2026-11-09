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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import java.time.LocalDate;
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
public final class ReviewViewModelTest {

  @Rule public final InstantTaskExecutorRule instantTaskExecutorRule =
      new InstantTaskExecutorRule();

  @Mock private CaptureRepository captureRepository;
  @Mock private ListItemRepository listItemRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private ShoppingListRepository listRepository;
  @Mock private CaptureImageStore imageStore;

  private final List<ReviewViewModel.ReviewUiEvent> events = new ArrayList<>();

  private ReviewViewModel viewModel;

  @Before
  public void setUp() {
    events.clear();
    CaptureSession session =
        new CaptureSession(
            "cap-1", "list-1", ItemSource.CAMERA_OCR, "/cache/captures/img.jpg", "json", 1000L,
            null, null);
    ShoppingList list =
        new ShoppingList(
            "list-1", "Weekly shop", LocalDate.of(2026, 11, 8), null, false, false, null, 1, 1);
    Store walmart =
        new Store(
            "store-1",
            StoreBrand.WALMART,
            "Walmart",
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            false,
            null,
            1,
            1);
    Store homeDepot =
        new Store(
            "store-2",
            StoreBrand.HOME_DEPOT,
            "Home Depot",
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            false,
            null,
            1,
            1);

    when(captureRepository.getById("cap-1")).thenReturn(session);
    when(captureRepository.decodeRecognizedLines("json"))
        .thenReturn(
            List.of(
                new RecognizedLine("milk", 0.95f), new RecognizedLine("eggs", 0.2f)));
    when(listRepository.getById("list-1")).thenReturn(list);
    when(storeRepository.getActive()).thenReturn(List.of(homeDepot, walmart));
    when(listItemRepository.nextSortPosition(anyString(), anyString())).thenReturn(0, 1);

    viewModel =
        new ReviewViewModel(
            captureRepository,
            listItemRepository,
            storeRepository,
            listRepository,
            imageStore,
            new FixedIdGenerator(),
            new FixedClock(1000L),
            new DirectExecutorService(),
            new SavedStateHandle(Map.of(NavArguments.CAPTURE_ID, "cap-1")));
    viewModel.getEvents().observeForever(event -> record(event.getContentIfNotHandled()));
  }

  private void record(ReviewViewModel.ReviewUiEvent event) {
    if (event != null) {
      events.add(event);
    }
  }

  @Test
  public void loadCaptureParsesLinesWithDefaults() {
    viewModel.loadCapture();

    assertEquals(2, viewModel.getLines().size());
    assertEquals("milk", viewModel.getLines().get(0).text);
    assertEquals(0.95f, viewModel.getLines().get(0).confidence, 0.0001f);
    assertTrue(viewModel.getLines().get(0).selected);
    assertEquals("store-1", viewModel.getLines().get(0).storeId);
    assertEquals("2026-11-08", viewModel.getLines().get(0).targetDate);
    assertEquals(ItemSource.CAMERA_OCR, viewModel.getSource());
    assertTrue(hasEvent(ReviewViewModel.ReviewUiEvent.CaptureLoaded.class));
  }

  @Test
  public void loadFailsWhenCaptureMissing() {
    when(captureRepository.getById("cap-1"))
        .thenThrow(new com.fastshoppr.app.domain.error.NotFoundException("missing"));

    viewModel.loadCapture();

    ReviewViewModel.ReviewUiEvent.LoadFailed failure = findEvent(ReviewViewModel.ReviewUiEvent.LoadFailed.class);
    assertNotNull(failure);
    assertEquals(UiError.CAPTURE_NOT_FOUND, failure.error());
  }

  @Test
  public void mergeSelectedJoinsInVisualOrderAtFirstPosition() {
    viewModel.loadCapture();
    viewModel.getLines().get(0).selected = true;
    viewModel.getLines().get(1).selected = true;

    viewModel.mergeSelected();

    assertEquals(1, viewModel.getLines().size());
    assertEquals("milk eggs", viewModel.getLines().get(0).text);
    assertTrue(viewModel.getLines().get(0).selected);
  }

  @Test
  public void splitLineSplitsAtFirstSpaceWithoutCursor() {
    viewModel.loadCapture();
    viewModel.getLines().get(0).text = "paper towels";

    viewModel.splitLine(0, -1);

    assertEquals(3, viewModel.getLines().size());
    assertEquals("paper", viewModel.getLines().get(0).text);
    assertEquals("towels", viewModel.getLines().get(1).text);
    assertEquals("eggs", viewModel.getLines().get(2).text);
  }

  @Test
  public void splitLineWithoutSplitPointReportsMessage() {
    viewModel.loadCapture();
    viewModel.splitLine(0, -1);

    assertNotNull(findEvent(ReviewViewModel.ReviewUiEvent.ShowMessage.class));
    ReviewViewModel.ReviewUiEvent.ShowMessage message =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowMessage.class);
    assertEquals(UiMessage.SPLIT_FAILED, message.message());
  }

  @Test
  public void saveSelectedCreatesItemsPreservingRawTextAndCompletesCapture() {
    viewModel.loadCapture();

    viewModel.saveSelected();

    ArgumentCaptor<ListItem> captor = ArgumentCaptor.forClass(ListItem.class);
    verify(listItemRepository, times(2)).insert(captor.capture());
    List<ListItem> saved = captor.getAllValues();
    assertEquals("milk", saved.get(0).getRawText());
    assertEquals(saved.get(0).getRawText(), saved.get(0).getDisplayName());
    assertEquals("eggs", saved.get(1).getRawText());
    assertEquals(ItemSource.CAMERA_OCR, saved.get(0).getSource());
    assertEquals(LocalDate.of(2026, 11, 8), saved.get(0).getTargetDate());
    assertEquals("store-1", saved.get(0).getStoreId());
    assertEquals(0, saved.get(0).getSortPosition());
    assertEquals(1, saved.get(1).getSortPosition());
    verify(captureRepository).complete(eq("cap-1"), eq(1000L));
    verify(imageStore).delete("/cache/captures/img.jpg");
    assertNotNull(findEvent(ReviewViewModel.ReviewUiEvent.SaveCompleted.class));
  }

  @Test
  public void saveSkipsBlankLinesAndRemovesThem() {
    viewModel.loadCapture();
    viewModel.getLines().get(0).text = "   ";

    viewModel.saveSelected();

    ArgumentCaptor<ListItem> captor = ArgumentCaptor.forClass(ListItem.class);
    verify(listItemRepository).insert(captor.capture());
    assertEquals("eggs", captor.getValue().getRawText());
    assertEquals(1, viewModel.getLines().size());
  }

  @Test
  public void saveWithoutSelectionFails() {
    viewModel.loadCapture();
    viewModel.deselectAll();

    viewModel.saveSelected();

    assertNotNull(findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class));
    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertEquals(UiError.NO_LINES_SELECTED, error.error());
    verify(listItemRepository, never()).insert(any());
    verify(captureRepository, never()).complete(anyString(), eq(1000L));
  }

  @Test
  public void saveRejectsOverlongLines() {
    viewModel.loadCapture();
    viewModel.getLines().get(0).text = "x".repeat(201);

    viewModel.saveSelected();

    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.RAW_TEXT_TOO_LONG, error.error());
    verify(listItemRepository, never()).insert(any());
  }

  @Test
  public void discardSoftDeletesCaptureAndDeletesImage() {
    viewModel.loadCapture();

    viewModel.discard();

    verify(captureRepository).softDelete("cap-1");
    verify(imageStore).delete("/cache/captures/img.jpg");
    assertNotNull(findEvent(ReviewViewModel.ReviewUiEvent.Discarded.class));
  }

  @Test
  public void confidenceFlagFollowsSourceThreshold() {
    viewModel.loadCapture();

    ReviewViewModel.ReviewLine high = viewModel.getLines().get(0);
    ReviewViewModel.ReviewLine low = viewModel.getLines().get(1);
    assertFalse(viewModel.isLowConfidence(high));
    assertTrue(viewModel.isLowConfidence(low));
  }

  @Test
  public void setLineStoreAndDateUpdateRow() {
    viewModel.loadCapture();

    viewModel.setLineStore(1, "store-2");
    viewModel.setLineDate(1, LocalDate.of(2026, 12, 1));

    assertEquals("store-2", viewModel.getLines().get(1).storeId);
    assertEquals("2026-12-01", viewModel.getLines().get(1).targetDate);
  }

  private boolean hasEvent(Class<? extends ReviewViewModel.ReviewUiEvent> type) {
    return findEvent(type) != null;
  }

  private <T extends ReviewViewModel.ReviewUiEvent> T findEvent(Class<T> type) {
    for (ReviewViewModel.ReviewUiEvent event : events) {
      if (type.isInstance(event)) {
        return type.cast(event);
      }
    }
    return null;
  }
}
