/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.data.capture.CapturedLinesCodec;
import com.fastshoppr.app.domain.ai.ItemNameCleaner;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.ai.NoOpItemNameCleaner;
import com.fastshoppr.app.domain.ai.RuleBasedItemNameCleaner;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.AiCleanupSource;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.usecase.SaveReviewedItemsUseCase;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingScheduler;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class ReviewViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Mock private CaptureRepository captureRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private ShoppingListRepository listRepository;
  @Mock private CaptureImageStore imageStore;
  @Mock private SettingsRepository settingsRepository;

  private final List<ReviewViewModel.ReviewUiEvent> events = new ArrayList<>();
  private final CapturedLinesCodec draftCodec = new CapturedLinesCodec(new Gson());

  private ReviewViewModel viewModel;

  @Before
  public void setUp() {
    events.clear();
    CaptureSession session =
        new CaptureSession(
            "cap-1",
            "list-1",
            ItemSource.CAMERA_OCR,
            "/cache/captures/img.jpg",
            "json",
            1000L,
            null,
            null);
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
            1,
            1);

    lenient().when(captureRepository.getById("cap-1")).thenReturn(session);
    lenient()
        .when(captureRepository.decodeRecognizedLines("json"))
        .thenReturn(List.of(new RecognizedLine("milk", 0.95f), new RecognizedLine("eggs", 0.2f)));
    lenient().when(captureRepository.decodeDraft(anyString())).thenReturn(List.of());
    lenient()
        .when(captureRepository.encodeDraft(anyList()))
        .thenAnswer(call -> draftCodec.encode(call.getArgument(0)));
    lenient().when(listRepository.getById("list-1")).thenReturn(list);
    lenient().when(storeRepository.getActive()).thenReturn(List.of(homeDepot, walmart));

    createViewModel(new NoOpItemNameCleaner());
  }

  private void createViewModel(ItemNameCleaner cleaner) {
    createViewModel(cleaner, new DirectExecutorService());
  }

  private ReviewViewModel createViewModel(ItemNameCleaner cleaner, ExecutorService executor) {
    OrganizingScheduler scheduler = mock(OrganizingScheduler.class);
    ReviewViewModel target =
        new ReviewViewModel(
            captureRepository,
            storeRepository,
            listRepository,
            imageStore,
            new SaveReviewedItemsUseCase(
                captureRepository,
                new FixedIdGenerator(),
                new FixedClock(1000L),
                new DirectExecutorService()),
            settingsRepository,
            new ItemNameCleanerCoordinator(
                cleaner, new RuleBasedItemNameCleaner(), settingsRepository),
            scheduler,
            executor,
            Runnable::run,
            new SavedStateHandle(Map.of(NavArguments.CAPTURE_ID, "cap-1")));
    target.getEvents().observeForever(event -> record(event.getContentIfNotHandled()));
    viewModel = target;
    return target;
  }

  private ItemNameCleaner availableCleaner(CleanItemResult result) {
    ItemNameCleaner cleaner = mock(ItemNameCleaner.class);
    when(cleaner.availability())
        .thenReturn(CompletableFuture.completedFuture(new AiAvailability.Available()));
    when(cleaner.clean(any(CleanItemRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(result));
    return cleaner;
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
    assertEquals("milk", viewModel.getLines().get(0).getText());
    assertEquals(0.95f, viewModel.getLines().get(0).getConfidence(), 0.0001f);
    assertTrue(viewModel.getLines().get(0).isSelected());
    assertEquals("store-1", viewModel.getLines().get(0).getStoreId());
    assertEquals("2026-11-08", viewModel.getLines().get(0).getTargetDate());
    assertEquals(ItemSource.CAMERA_OCR, viewModel.getSource());
    assertTrue(hasEvent(ReviewViewModel.ReviewUiEvent.CaptureLoaded.class));
  }

  @Test
  public void loadFailsWhenCaptureMissing() {
    when(captureRepository.getById("cap-1"))
        .thenThrow(new com.fastshoppr.app.domain.error.NotFoundException("missing"));

    viewModel.loadCapture();

    ReviewViewModel.ReviewUiEvent.LoadFailed failure =
        findEvent(ReviewViewModel.ReviewUiEvent.LoadFailed.class);
    assertNotNull(failure);
    assertEquals(UiError.CAPTURE_NOT_FOUND, failure.error());
  }

  @Test
  public void mergeSelectedJoinsInVisualOrderAtFirstPosition() {
    viewModel.loadCapture();
    viewModel.setLineSelected(0, true);
    viewModel.setLineSelected(1, true);

    viewModel.mergeSelected();

    assertEquals(1, viewModel.getLines().size());
    assertEquals("milk eggs", viewModel.getLines().get(0).getText());
    assertTrue(viewModel.getLines().get(0).isSelected());
  }

  @Test
  public void splitLineSplitsAtFirstSpaceWithoutCursor() {
    viewModel.loadCapture();
    viewModel.setLineText(0, "paper towels");

    viewModel.splitLine(0, -1);

    assertEquals(3, viewModel.getLines().size());
    assertEquals("paper", viewModel.getLines().get(0).getText());
    assertEquals("towels", viewModel.getLines().get(1).getText());
    assertEquals("eggs", viewModel.getLines().get(2).getText());
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
  @SuppressWarnings("unchecked")
  public void saveSelectedCreatesItemsPreservingRawTextAndCompletesCapture() {
    viewModel.loadCapture();

    viewModel.saveSelected();

    ArgumentCaptor<List<ListItem>> captor = ArgumentCaptor.forClass(List.class);
    verify(captureRepository).saveReviewed(eq("cap-1"), captor.capture(), eq(1000L));
    List<ListItem> saved = captor.getValue();
    assertEquals(2, saved.size());
    assertEquals("milk", saved.get(0).getRawText());
    assertEquals(saved.get(0).getRawText(), saved.get(0).getDisplayName());
    assertEquals("eggs", saved.get(1).getRawText());
    assertEquals(ItemSource.CAMERA_OCR, saved.get(0).getSource());
    assertEquals(LocalDate.of(2026, 11, 8), saved.get(0).getTargetDate());
    assertEquals("store-1", saved.get(0).getStoreId());
    verify(imageStore).delete("/cache/captures/img.jpg");
    assertNotNull(findEvent(ReviewViewModel.ReviewUiEvent.SaveCompleted.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void saveSkipsBlankLinesAndRemovesThem() {
    viewModel.loadCapture();
    viewModel.setLineText(0, "   ");

    viewModel.saveSelected();

    ArgumentCaptor<List<ListItem>> captor = ArgumentCaptor.forClass(List.class);
    verify(captureRepository).saveReviewed(eq("cap-1"), captor.capture(), eq(1000L));
    assertEquals(1, captor.getValue().size());
    assertEquals("eggs", captor.getValue().get(0).getRawText());
    assertEquals(1, viewModel.getLines().size());
  }

  @Test
  public void saveRejectsOverlongAcceptedDisplayName() {
    viewModel.loadCapture();
    viewModel.acceptSuggestionAs(
        new ReviewViewModel.CleanupSuggestion(
            0, "milk", "Milk", 0.9f, null, AiCleanupSource.ON_DEVICE_AI),
        "x".repeat(201));

    viewModel.saveSelected();

    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.RAW_TEXT_TOO_LONG, error.error());
    verify(captureRepository, never()).saveReviewed(anyString(), any(), anyLong());
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
    verify(captureRepository, never()).saveReviewed(anyString(), any(), anyLong());
  }

  @Test
  public void saveRejectsOverlongLines() {
    viewModel.loadCapture();
    viewModel.setLineText(0, "x".repeat(201));

    viewModel.saveSelected();

    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.RAW_TEXT_TOO_LONG, error.error());
    verify(captureRepository, never()).saveReviewed(anyString(), any(), anyLong());
  }

  @Test
  public void saveFailureReportsErrorAndDoesNotDeleteImage() {
    viewModel.loadCapture();
    doThrow(new RuntimeException("db down"))
        .when(captureRepository)
        .saveReviewed(eq("cap-1"), any(), eq(1000L));

    viewModel.saveSelected();

    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.SAVE_FAILED, error.error());
    verify(imageStore, never()).delete("/cache/captures/img.jpg");
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
  public void discardFailureReportsErrorAndDoesNotNavigate() {
    viewModel.loadCapture();
    doThrow(new RuntimeException("delete failed")).when(captureRepository).softDelete("cap-1");

    viewModel.discard();

    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.DISCARD_FAILED, error.error());
    assertTrue(findEvent(ReviewViewModel.ReviewUiEvent.Discarded.class) == null);
  }

  @Test
  public void reloadCaptureWaitsForPendingWritesAndReparsesDraft() {
    viewModel.loadCapture();
    viewModel.setLineText(0, "pending edit");
    when(captureRepository.decodeDraft("json"))
        .thenReturn(
            List.of(
                new CapturedLine("from ink", null, true, 0.8f, false, "store-1", "2026-11-08")));

    viewModel.reloadCapture();

    assertEquals(1, viewModel.getLineCount());
    assertEquals("from ink", viewModel.getLine(0).getText());
    assertTrue(hasEvent(ReviewViewModel.ReviewUiEvent.CaptureLoaded.class));
  }

  @Test
  public void reloadInvalidatesStaleLoad() {
    ManualExecutorService executor = new ManualExecutorService();
    when(captureRepository.decodeDraft("json"))
        .thenReturn(
            List.of(
                new CapturedLine("old draft", null, true, 0.8f, false, "store-1", "2026-11-08")));
    ReviewViewModel target = createViewModel(new NoOpItemNameCleaner(), executor);

    target.loadCapture();
    when(captureRepository.decodeDraft("json"))
        .thenReturn(
            List.of(
                new CapturedLine("fresh draft", null, true, 0.9f, false, "store-1", "2026-11-08")));
    target.reloadCapture();
    executor.drain();

    assertEquals(1, target.getLineCount());
    assertEquals("fresh draft", target.getLine(0).getText());
  }

  @Test
  public void draftWriteFailureReportsSaveFailed() {
    viewModel.loadCapture();
    doThrow(new RuntimeException("db down"))
        .when(captureRepository)
        .updateDraft(eq("cap-1"), anyString(), any());

    viewModel.setLineText(0, "edited");

    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.SAVE_FAILED, error.error());
  }

  @Test
  public void discardWaitsForQueuedDraftWriteBeforeSoftDelete() {
    ManualExecutorService executor = new ManualExecutorService();
    List<String> order = Collections.synchronizedList(new ArrayList<>());
    doAnswer(
            call -> {
              order.add("delete");
              return null;
            })
        .when(captureRepository)
        .softDelete("cap-1");
    ReviewViewModel target = createViewModel(new NoOpItemNameCleaner(), executor);

    target.loadCapture();
    executor.drain();
    target.setLineText(0, "edited");
    target.discard();

    assertTrue(order.isEmpty());
    verify(captureRepository, never()).softDelete("cap-1");

    // The queued write task runs first but is suppressed because discard was requested.
    executor.runNext();
    assertTrue(order.isEmpty());
    verify(captureRepository, never()).softDelete("cap-1");

    // Only after that task completes does the discard continuation delete the capture.
    executor.runNext();
    assertEquals(List.of("delete"), order);
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

    assertEquals("store-2", viewModel.getLines().get(1).getStoreId());
    assertEquals("2026-12-01", viewModel.getLines().get(1).getTargetDate());
  }

  @Test
  public void cleanupStaysIdleWhenDisabled() {
    ItemNameCleaner cleaner = mock(ItemNameCleaner.class);
    createViewModel(cleaner);
    viewModel.loadCapture();

    viewModel.startCleanup();

    assertEquals(ReviewViewModel.CleanupStatus.IDLE, cleanupState().getStatus());
    assertFalse(cleanupState().isAiEnabled());
    assertTrue(cleanupState().getSuggestions().isEmpty());
    verify(cleaner, never()).clean(any());
  }

  @Test
  public void cleanupAcceptSetsDisplayNameWithoutChangingRawText() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    createViewModel(availableCleaner(new CleanItemResult.Suggested("Milk", 0.9f, "Capitalized")));
    viewModel.loadCapture();

    viewModel.startCleanup();

    assertEquals(ReviewViewModel.CleanupStatus.FINISHED, cleanupState().getStatus());
    assertEquals(2, cleanupState().getSuggestions().size());
    ReviewViewModel.CleanupSuggestion suggestion = suggestionFor("milk");
    assertNotNull(suggestion);
    assertEquals("Milk", suggestion.getSuggestedName());
    assertEquals(AiCleanupSource.ON_DEVICE_AI, suggestion.getSource());

    viewModel.acceptSuggestion(suggestion);

    assertEquals("milk", viewModel.getLine(0).getText());
    assertEquals("Milk", viewModel.getLine(0).getDisplayName());
    assertEquals(1, cleanupState().getSuggestions().size());
    assertNull(suggestionFor("milk"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void savePersistsAcceptedDisplayNameSeparatelyFromRawText() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    createViewModel(availableCleaner(new CleanItemResult.Suggested("Milk", 0.9f, "Capitalized")));
    viewModel.loadCapture();
    viewModel.startCleanup();

    viewModel.acceptSuggestion(suggestionFor("milk"));
    viewModel.saveSelected();

    ArgumentCaptor<List<ListItem>> captor = ArgumentCaptor.forClass(List.class);
    verify(captureRepository).saveReviewed(eq("cap-1"), captor.capture(), eq(1000L));
    assertEquals("milk", captor.getValue().get(0).getRawText());
    assertEquals("Milk", captor.getValue().get(0).getDisplayName());
  }

  @Test
  public void acceptAllAppliesEverySuggestion() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    ItemNameCleaner cleaner = mock(ItemNameCleaner.class);
    when(cleaner.availability())
        .thenReturn(CompletableFuture.completedFuture(new AiAvailability.Available()));
    when(cleaner.clean(any(CleanItemRequest.class)))
        .thenAnswer(
            invocation -> {
              String rawText = invocation.getArgument(0, CleanItemRequest.class).getRawText();
              String name = "milk".equals(rawText) ? "Milk" : "Eggs";
              return CompletableFuture.completedFuture(
                  new CleanItemResult.Suggested(name, 0.85f, "Cleaned"));
            });
    createViewModel(cleaner);
    viewModel.loadCapture();
    viewModel.startCleanup();

    viewModel.acceptAllSuggestions();

    assertEquals("Milk", viewModel.getLine(0).getDisplayName());
    assertEquals("Eggs", viewModel.getLine(1).getDisplayName());
    assertEquals("milk", viewModel.getLine(0).getText());
    assertEquals("eggs", viewModel.getLine(1).getText());
  }

  @Test
  public void editedSuggestionUsesUserTextAndKeepsRawText() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    createViewModel(availableCleaner(new CleanItemResult.Suggested("Milk", 0.9f, "Capitalized")));
    viewModel.loadCapture();
    viewModel.startCleanup();

    viewModel.acceptSuggestionAs(suggestionFor("milk"), "Organic milk");

    assertEquals("milk", viewModel.getLine(0).getText());
    assertEquals("Organic milk", viewModel.getLine(0).getDisplayName());
  }

  @Test
  public void dismissedItemsAreNotSuggestedAgain() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    createViewModel(availableCleaner(new CleanItemResult.Suggested("Milk", 0.9f, "Cleaned")));
    viewModel.loadCapture();
    viewModel.startCleanup();

    viewModel.dismissSuggestion(suggestionFor("milk"));
    viewModel.acceptAllSuggestions();
    viewModel.startCleanup();

    assertTrue(cleanupState().getSuggestions().isEmpty());
    assertEquals(ReviewViewModel.CleanupStatus.FINISHED, cleanupState().getStatus());
    ReviewViewModel.ReviewUiEvent.ShowMessage message =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowMessage.class);
    assertNotNull(message);
    assertEquals(UiMessage.NO_CLEANUP_CANDIDATES, message.message());
  }

  @Test
  public void cleanupCapsEachRunAtFiveItems() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    ItemNameCleaner cleaner =
        availableCleaner(new CleanItemResult.Suggested("Cleaned", 0.8f, "Rules"));
    createViewModel(cleaner);
    viewModel.loadCapture();
    for (int index = 0; index < 5; index++) {
      viewModel.addLine("item-" + index);
    }

    viewModel.startCleanup();

    assertEquals(5, cleanupState().getSuggestions().size());
    verify(cleaner, times(5)).clean(any(CleanItemRequest.class));
  }

  @Test
  public void cleanupFallsBackToRulesWhenAiUnavailable() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    ItemNameCleaner cleaner = mock(ItemNameCleaner.class);
    when(cleaner.availability())
        .thenReturn(CompletableFuture.completedFuture(new AiAvailability.Unavailable()));
    createViewModel(cleaner);
    viewModel.loadCapture();
    viewModel.addLine("tp");

    viewModel.startCleanup();

    ReviewViewModel.CleanupSuggestion suggestion = suggestionFor("tp");
    assertNotNull(suggestion);
    assertEquals("Toilet paper", suggestion.getSuggestedName());
    assertEquals(AiCleanupSource.RULES, suggestion.getSource());
    verify(cleaner, never()).clean(any());
  }

  @Test
  public void addLineWithTextAddsLocalReviewLineAndPersistsDraft() {
    viewModel.loadCapture();

    viewModel.addLine("bread");

    assertEquals(3, viewModel.getLineCount());
    ReviewViewModel.ReviewLine added = viewModel.getLine(2);
    assertEquals("bread", added.getText());
    assertFalse(added.isSelected());
    assertTrue(added.isManual());
    assertEquals("store-1", added.getStoreId());
    assertEquals("2026-11-08", added.getTargetDate());
    assertTrue(hasEvent(ReviewViewModel.ReviewUiEvent.LinesChanged.class));
    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(captureRepository).updateDraft(eq("cap-1"), json.capture(), any());
    List<CapturedLine> draft = draftCodec.decode(json.getValue());
    assertEquals(3, draft.size());
    assertEquals("bread", draft.get(2).getText());
  }

  @Test
  public void addLineReturnsToInkAndPersistsCurrentEdits() {
    viewModel.loadCapture();
    viewModel.setLineText(0, "organic milk");
    viewModel.setLineSelected(1, false);
    viewModel.setLineStore(0, "store-2");
    viewModel.setLineDate(0, LocalDate.of(2026, 12, 1));
    viewModel.acceptSuggestionAs(
        new ReviewViewModel.CleanupSuggestion(
            0, "organic milk", "Organic Milk", 0.9f, "test", AiCleanupSource.RULES),
        "Organic Milk");

    viewModel.addLine();

    ReviewViewModel.ReviewUiEvent.NavigateToInk event =
        findEvent(ReviewViewModel.ReviewUiEvent.NavigateToInk.class);
    assertNotNull(event);
    assertEquals("list-1", event.listId());
    assertEquals("cap-1", event.captureId());
    verify(captureRepository, never()).saveReviewed(anyString(), any(), anyLong());
    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(captureRepository, atLeastOnce()).updateDraft(eq("cap-1"), json.capture(), any());
    List<CapturedLine> draft =
        draftCodec.decode(json.getAllValues().get(json.getAllValues().size() - 1));
    assertEquals("organic milk", draft.get(0).getText());
    assertEquals("Organic Milk", draft.get(0).getDisplayName());
    assertTrue(draft.get(0).isSelected());
    assertEquals("store-2", draft.get(0).getStoreId());
    assertEquals("2026-12-01", draft.get(0).getTargetDate());
    assertFalse(draft.get(1).isSelected());
  }

  @Test
  public void addLineWithoutLoadedCaptureReportsError() {
    viewModel.addLine();

    ReviewViewModel.ReviewUiEvent.ShowError error =
        findEvent(ReviewViewModel.ReviewUiEvent.ShowError.class);
    assertNotNull(error);
    assertEquals(UiError.CAPTURE_NOT_FOUND, error.error());
    verify(captureRepository, never()).updateDraft(anyString(), anyString(), any());
  }

  private ReviewViewModel.CleanupState cleanupState() {
    return viewModel.getCleanupState().getValue();
  }

  private ReviewViewModel.CleanupSuggestion suggestionFor(String rawText) {
    for (ReviewViewModel.CleanupSuggestion suggestion : cleanupState().getSuggestions()) {
      if (rawText.equals(suggestion.getRawText())) {
        return suggestion;
      }
    }
    return null;
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

  /** Deterministic executor that lets a test observe the order of queued draft writes. */
  private static final class ManualExecutorService extends AbstractExecutorService {

    private final Deque<Runnable> tasks = new ArrayDeque<>();
    private boolean shutdown;

    @Override
    public void execute(Runnable command) {
      if (shutdown) {
        throw new IllegalStateException("executor shut down");
      }
      tasks.addLast(command);
    }

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown = true;
      List<Runnable> pending = new ArrayList<>(tasks);
      tasks.clear();
      return pending;
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public boolean isTerminated() {
      return shutdown && tasks.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return isTerminated();
    }

    private void runNext() {
      Runnable task = tasks.pollFirst();
      if (task == null) {
        throw new AssertionError("no queued task");
      }
      task.run();
    }

    private void drain() {
      while (!tasks.isEmpty()) {
        runNext();
      }
    }
  }
}
