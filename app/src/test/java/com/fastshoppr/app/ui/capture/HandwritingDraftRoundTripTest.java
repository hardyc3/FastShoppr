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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.data.capture.CapturedLinesCodec;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.ai.NoOpItemNameCleaner;
import com.fastshoppr.app.domain.ai.RuleBasedItemNameCleaner;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.AiCleanupSource;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.InkData;
import com.fastshoppr.app.domain.model.InkPoint;
import com.fastshoppr.app.domain.model.InkStroke;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.InkRecognitionRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.SaveReviewedItemsUseCase;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingScheduler;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * End-to-end draft persistence across the handwriting and review screens, using a fake repository
 * that persists the same JSON as production.
 */
public final class HandwritingDraftRoundTripTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  private final CapturedLinesCodec codec = new CapturedLinesCodec(new Gson());
  private final InMemoryCaptureRepository captureRepository = new InMemoryCaptureRepository(codec);
  private final FakeInkRecognitionRepository inkRepository = new FakeInkRecognitionRepository();
  private final DirectExecutorService executor = new DirectExecutorService();
  private final List<ReviewViewModel.ReviewUiEvent> reviewEvents = new ArrayList<>();

  private SettingsRepository settingsRepository;

  @Before
  public void setUp() {
    settingsRepository = mock(SettingsRepository.class);
    ShoppingListRepository listRepository = mock(ShoppingListRepository.class);
    StoreRepository storeRepository = mock(StoreRepository.class);
    lenient().when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    lenient()
        .when(listRepository.getById("list-1"))
        .thenReturn(
            new ShoppingList(
                "list-1",
                "Weekly shop",
                LocalDate.of(2026, 11, 8),
                null,
                false,
                false,
                null,
                1,
                1));
    lenient()
        .when(storeRepository.getActive())
        .thenReturn(
            List.of(store("store-1", StoreBrand.WALMART), store("store-2", StoreBrand.HOME_DEPOT)));
    reviewRepositories.listRepository = listRepository;
    reviewRepositories.storeRepository = storeRepository;
  }

  private final ReviewRepositories reviewRepositories = new ReviewRepositories();

  @Test
  public void handwritingDraftSurvivesReviewEditsAndReturnTrip() {
    String captureId = saveHandwriting("milk", "eggs");

    ReviewViewModel review = createReview(captureId);
    review.loadCapture();

    assertEquals(2, review.getLineCount());
    assertEquals("milk", review.getLine(0).getText());
    assertTrue(review.getLine(0).isSelected());
    assertEquals("store-1", review.getLine(0).getStoreId());
    assertEquals("2026-11-08", review.getLine(0).getTargetDate());

    ReviewViewModel.CleanupSuggestion suggestion =
        new ReviewViewModel.CleanupSuggestion(
            0, "milk", "Almond Milk", 0.9f, "test", AiCleanupSource.RULES);
    review.acceptSuggestionAs(suggestion, "Almond Milk");
    review.setLineSelected(0, false);
    review.setLineStore(0, "store-2");
    review.setLineDate(0, LocalDate.of(2026, 11, 20));
    review.addLine("bread");
    review.addLine();

    assertNotNull(findReviewEvent(ReviewViewModel.ReviewUiEvent.NavigateToInk.class));

    ReviewViewModel reloaded = createReview(captureId);
    reloaded.loadCapture();

    assertEquals(3, reloaded.getLineCount());
    assertEquals("milk", reloaded.getLine(0).getText());
    assertEquals("Almond Milk", reloaded.getLine(0).getDisplayName());
    assertFalse(reloaded.getLine(0).isSelected());
    assertEquals("store-2", reloaded.getLine(0).getStoreId());
    assertEquals("2026-11-20", reloaded.getLine(0).getTargetDate());
    assertEquals("eggs", reloaded.getLine(1).getText());
    assertTrue(reloaded.getLine(1).isSelected());
    assertEquals("store-1", reloaded.getLine(1).getStoreId());
    assertEquals("2026-11-08", reloaded.getLine(1).getTargetDate());
    assertEquals("bread", reloaded.getLine(2).getText());
    assertFalse(reloaded.getLine(2).isSelected());
    assertTrue(reloaded.getLine(2).isManual());
  }

  @Test
  public void addLinePersistsDraftAndNavigatesBackToInk() {
    String captureId = saveHandwriting("milk");
    ReviewViewModel review = createReview(captureId);
    review.loadCapture();

    review.setLineText(0, "oat milk");
    review.addLine();

    List<CapturedLine> persisted =
        codec.decode(captureRepository.getById(captureId).getRecognizedLinesJson());
    assertEquals(1, persisted.size());
    assertEquals("oat milk", persisted.get(0).getText());
    assertNotNull(findReviewEvent(ReviewViewModel.ReviewUiEvent.NavigateToInk.class));
  }

  private String saveHandwriting(String... texts) {
    inkRepository.result = new ArrayList<>();
    for (String text : texts) {
      inkRepository.result.add(new RecognizedLine(text, 0.9f));
    }
    InkCaptureViewModel ink =
        new InkCaptureViewModel(
            captureRepository,
            inkRepository,
            (IdGenerator) () -> "cap-1",
            (Clock) () -> 1000L,
            executor,
            new SavedStateHandle(Map.of(NavArguments.LIST_ID, "list-1")));
    List<InkCaptureViewModel.InkUiEvent> inkEvents = new ArrayList<>();
    ink.getEvents().observeForever(wrapper -> inkEvents.add(wrapper.getContentIfNotHandled()));
    ink.save(sampleInk());
    InkCaptureViewModel.InkUiEvent.NavigateToReview event =
        findInkEvent(inkEvents, InkCaptureViewModel.InkUiEvent.NavigateToReview.class);
    assertNotNull(event);
    return event.captureId();
  }

  private ReviewViewModel createReview(String captureId) {
    reviewEvents.clear();
    ReviewViewModel viewModel =
        new ReviewViewModel(
            captureRepository,
            reviewRepositories.storeRepository,
            reviewRepositories.listRepository,
            mock(CaptureImageStore.class),
            mock(SaveReviewedItemsUseCase.class),
            settingsRepository,
            new ItemNameCleanerCoordinator(
                new NoOpItemNameCleaner(), new RuleBasedItemNameCleaner(), settingsRepository),
            mock(OrganizingScheduler.class),
            executor,
            Runnable::run,
            new SavedStateHandle(Map.of(NavArguments.CAPTURE_ID, captureId)));
    viewModel
        .getEvents()
        .observeForever(wrapper -> reviewEvents.add(wrapper.getContentIfNotHandled()));
    return viewModel;
  }

  private <T extends ReviewViewModel.ReviewUiEvent> T findReviewEvent(Class<T> type) {
    for (ReviewViewModel.ReviewUiEvent event : reviewEvents) {
      if (event != null && type.isInstance(event)) {
        return type.cast(event);
      }
    }
    return null;
  }

  private static Store store(String id, StoreBrand brand) {
    return new Store(
        id, brand, brand.name(), null, null, null, null, null, null, false, false, false, 1, 1);
  }

  private static InkData sampleInk() {
    return new InkData(
        100,
        200,
        List.of(
            new InkStroke(List.of(new InkPoint(10f, 20f, 1000L), new InkPoint(14f, 24f, 1100L)))));
  }

  private static <T extends InkCaptureViewModel.InkUiEvent> T findInkEvent(
      List<InkCaptureViewModel.InkUiEvent> events, Class<T> type) {
    for (InkCaptureViewModel.InkUiEvent event : events) {
      if (event != null && type.isInstance(event)) {
        return type.cast(event);
      }
    }
    return null;
  }

  private static final class ReviewRepositories {
    private ShoppingListRepository listRepository;
    private StoreRepository storeRepository;
  }

  private static final class FakeInkRecognitionRepository implements InkRecognitionRepository {

    private List<RecognizedLine> result = List.of();

    @Override
    public void prepareModel(ModelListener listener) {
      listener.onReady();
    }

    @Override
    public CompletableFuture<List<RecognizedLine>> recognize(InkData ink) {
      return CompletableFuture.completedFuture(result);
    }

    @Override
    public void close() {}
  }

  private static final class InMemoryCaptureRepository implements CaptureRepository {

    private final CapturedLinesCodec codec;
    private final Map<String, CaptureSession> sessions = new HashMap<>();

    private InMemoryCaptureRepository(CapturedLinesCodec codec) {
      this.codec = codec;
    }

    @Override
    public LiveData<List<CaptureSession>> observeByList(String listId) {
      return new MutableLiveData<>(List.of());
    }

    @Override
    public List<CaptureSession> getByList(String listId) {
      return List.of();
    }

    @Override
    public List<CaptureSession> getByListIncludingDeleted(String listId) {
      return List.of();
    }

    @Override
    public CaptureSession getById(String id) {
      return sessions.get(id);
    }

    @Override
    public void insert(CaptureSession session) {
      sessions.put(session.getId(), session);
    }

    @Override
    public void complete(String id, long completedAt) {}

    @Override
    public void saveReviewed(String captureId, List<ListItem> items, long completedAt) {}

    @Override
    public void softDelete(String id) {
      sessions.remove(id);
    }

    @Override
    public int purgeDeletedBefore(long cutoffMillis) {
      return 0;
    }

    @Override
    public String encodeRecognizedLines(List<RecognizedLine> lines) {
      return "";
    }

    @Override
    public List<RecognizedLine> decodeRecognizedLines(String json) {
      return List.of();
    }

    @Override
    public String encodeDraft(List<CapturedLine> lines) {
      return codec.encode(lines);
    }

    @Override
    public List<CapturedLine> decodeDraft(String json) {
      return codec.decode(json);
    }

    @Override
    public void updateDraft(String captureId, String draftJson, Long completedAt) {
      CaptureSession current = sessions.get(captureId);
      if (current == null) {
        return;
      }
      sessions.put(
          captureId,
          new CaptureSession(
              current.getId(),
              current.getListId(),
              current.getSource(),
              current.getImageUri(),
              draftJson,
              current.getCreatedAt(),
              completedAt == null ? current.getCompletedAt() : completedAt,
              current.getDeletedAt()));
    }
  }
}
