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
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.domain.ai.CleanupOutcome;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.concurrent.MainThreadExecutor;
import com.fastshoppr.app.domain.date.LocalDateTimes;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.AiCleanupSource;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.RecognitionConfidence;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ReviewedItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.SaveReviewedItemsUseCase;
import com.fastshoppr.app.ui.common.DateText;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StoreOrdering;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class ReviewViewModel extends ViewModel {

  private static final int MAX_TEXT_LENGTH = 200;
  private static final int MAX_CLEANUP_ITEMS = 5;
  private static final int MAX_CLEANUP_CONTEXT = 5;

  public sealed interface ReviewUiEvent
      permits ReviewUiEvent.CaptureLoaded,
          ReviewUiEvent.LoadFailed,
          ReviewUiEvent.LinesChanged,
          ReviewUiEvent.SelectionChanged,
          ReviewUiEvent.NavigateToInk,
          ReviewUiEvent.SaveCompleted,
          ReviewUiEvent.Discarded,
          ReviewUiEvent.ShowError,
          ReviewUiEvent.ShowMessage {

    record CaptureLoaded() implements ReviewUiEvent {}

    record LoadFailed(UiError error) implements ReviewUiEvent {}

    record LinesChanged() implements ReviewUiEvent {}

    record SelectionChanged() implements ReviewUiEvent {}

    record NavigateToInk(String listId, String captureId) implements ReviewUiEvent {}

    record SaveCompleted() implements ReviewUiEvent {}

    record Discarded() implements ReviewUiEvent {}

    record ShowError(UiError error) implements ReviewUiEvent {}

    record ShowMessage(UiMessage message) implements ReviewUiEvent {}
  }

  public static final class ReviewLine {
    private final long lineId;
    private final String text;
    private final String displayName;
    private final boolean selected;
    private final float confidence;
    private final boolean manual;
    private final String storeId;
    private final String targetDate;

    public ReviewLine(
        String text,
        boolean selected,
        float confidence,
        boolean manual,
        String storeId,
        String targetDate) {
      this(0L, text, null, selected, confidence, manual, storeId, targetDate);
    }

    public ReviewLine(
        String text,
        String displayName,
        boolean selected,
        float confidence,
        boolean manual,
        String storeId,
        String targetDate) {
      this(0L, text, displayName, selected, confidence, manual, storeId, targetDate);
    }

    ReviewLine(
        long lineId,
        String text,
        String displayName,
        boolean selected,
        float confidence,
        boolean manual,
        String storeId,
        String targetDate) {
      this.lineId = lineId;
      this.text = text;
      this.displayName = displayName;
      this.selected = selected;
      this.confidence = confidence;
      this.manual = manual;
      this.storeId = storeId;
      this.targetDate = targetDate;
    }

    public long getLineId() {
      return lineId;
    }

    public String getText() {
      return text;
    }

    /** Accepted user-facing name; null means the written text is used unchanged. */
    public String getDisplayName() {
      return displayName;
    }

    public boolean isSelected() {
      return selected;
    }

    public float getConfidence() {
      return confidence;
    }

    public boolean isManual() {
      return manual;
    }

    public String getStoreId() {
      return storeId;
    }

    public String getTargetDate() {
      return targetDate;
    }

    ReviewLine withText(String value) {
      return new ReviewLine(lineId, value, null, selected, confidence, manual, storeId, targetDate);
    }

    ReviewLine withSelected(boolean value) {
      return new ReviewLine(
          lineId, text, displayName, value, confidence, manual, storeId, targetDate);
    }

    ReviewLine withStoreId(String value) {
      return new ReviewLine(
          lineId, text, displayName, selected, confidence, manual, value, targetDate);
    }

    ReviewLine withTargetDate(String value) {
      return new ReviewLine(
          lineId, text, displayName, selected, confidence, manual, storeId, value);
    }

    ReviewLine withDisplayName(String value) {
      return new ReviewLine(lineId, text, value, selected, confidence, manual, storeId, targetDate);
    }
  }

  public enum CleanupStatus {
    IDLE,
    CHECKING,
    RUNNING,
    FINISHED
  }

  /** One before/after suggestion that has not yet been accepted or dismissed. */
  public static final class CleanupSuggestion {
    private final int lineIndex;
    private final String rawText;
    private final String suggestedName;
    private final float confidence;
    private final String reason;
    private final AiCleanupSource source;

    CleanupSuggestion(
        int lineIndex,
        String rawText,
        String suggestedName,
        float confidence,
        String reason,
        AiCleanupSource source) {
      this.lineIndex = lineIndex;
      this.rawText = rawText;
      this.suggestedName = suggestedName;
      this.confidence = confidence;
      this.reason = reason;
      this.source = source;
    }

    public int getLineIndex() {
      return lineIndex;
    }

    public String getRawText() {
      return rawText;
    }

    public String getSuggestedName() {
      return suggestedName;
    }

    public float getConfidence() {
      return confidence;
    }

    public String getReason() {
      return reason;
    }

    public AiCleanupSource getSource() {
      return source;
    }
  }

  /** Immutable state for the "Clean up names" action and suggestion sheet. */
  public static final class CleanupState {
    private final CleanupStatus status;
    private final boolean aiEnabled;
    private final AiAvailability availability;
    private final List<CleanupSuggestion> suggestions;

    CleanupState(
        CleanupStatus status,
        boolean aiEnabled,
        AiAvailability availability,
        List<CleanupSuggestion> suggestions) {
      this.status = status;
      this.aiEnabled = aiEnabled;
      this.availability = availability;
      this.suggestions = List.copyOf(suggestions);
    }

    public CleanupStatus getStatus() {
      return status;
    }

    public boolean isAiEnabled() {
      return aiEnabled;
    }

    public AiAvailability getAvailability() {
      return availability;
    }

    public List<CleanupSuggestion> getSuggestions() {
      return suggestions;
    }

    public boolean isRunning() {
      return status == CleanupStatus.CHECKING || status == CleanupStatus.RUNNING;
    }

    public boolean isFinished() {
      return status == CleanupStatus.FINISHED;
    }
  }

  private final CaptureRepository captureRepository;
  private final StoreRepository storeRepository;
  private final ShoppingListRepository listRepository;
  private final CaptureImageStore imageStore;
  private final SaveReviewedItemsUseCase saveReviewedItemsUseCase;
  private final SettingsRepository settingsRepository;
  private final ItemNameCleanerCoordinator cleanerCoordinator;
  private final OrganizingScheduler organizingScheduler;
  private final ExecutorService executor;
  private final MainThreadExecutor mainThread;
  private final Clock clock;

  private final MutableLiveData<SingleEvent<ReviewUiEvent>> events = new MutableLiveData<>();
  private final MutableLiveData<CleanupState> cleanupState = new MutableLiveData<>();

  private final String captureId;
  private final List<ReviewLine> lines = new ArrayList<>();
  private long nextLineId = 1L;
  private final List<CleanupSuggestion> cleanupSuggestions = new ArrayList<>();
  private final Set<String> dismissedCleanupTexts = new HashSet<>();

  private CaptureSession currentCapture;
  private ShoppingList currentList;
  private List<Store> stores = List.of();
  private ItemSource source = ItemSource.CAPTURE;
  private LocalDate defaultDate;
  private int selectedCount;
  private boolean isSaving;
  private boolean loading;
  private boolean loaded;
  private int loadGeneration;
  private CompletableFuture<Void> draftWrites = CompletableFuture.completedFuture(null);
  private boolean draftDiscarded;
  private boolean aiCleanupEnabled;
  private boolean screenStarted;
  private volatile CleanupStatus cleanupStatus = CleanupStatus.IDLE;
  private volatile AiAvailability cleanupAvailability;
  private volatile CompletableFuture<AiAvailability> availabilityFuture;
  private volatile CompletableFuture<CleanupOutcome> cleanFuture;
  private volatile int cleanupGeneration;
  private volatile int availabilityGeneration;

  @Inject
  public ReviewViewModel(
      CaptureRepository captureRepository,
      StoreRepository storeRepository,
      ShoppingListRepository listRepository,
      CaptureImageStore imageStore,
      SaveReviewedItemsUseCase saveReviewedItemsUseCase,
      SettingsRepository settingsRepository,
      ItemNameCleanerCoordinator cleanerCoordinator,
      OrganizingScheduler organizingScheduler,
      ExecutorService executor,
      MainThreadExecutor mainThread,
      Clock clock,
      SavedStateHandle savedStateHandle) {
    this.captureRepository = captureRepository;
    this.storeRepository = storeRepository;
    this.listRepository = listRepository;
    this.imageStore = imageStore;
    this.saveReviewedItemsUseCase = saveReviewedItemsUseCase;
    this.settingsRepository = settingsRepository;
    this.cleanerCoordinator = cleanerCoordinator;
    this.organizingScheduler = organizingScheduler;
    this.executor = executor;
    this.mainThread = mainThread;
    this.clock = clock;
    this.captureId = savedStateHandle.get(NavArguments.CAPTURE_ID);
    cleanupState.setValue(currentCleanupState());
  }

  public LiveData<SingleEvent<ReviewUiEvent>> getEvents() {
    return events;
  }

  public LiveData<CleanupState> getCleanupState() {
    return cleanupState;
  }

  public boolean isAiCleanupEnabled() {
    return aiCleanupEnabled;
  }

  public String getCaptureId() {
    return captureId;
  }

  public List<ReviewLine> getLines() {
    return List.copyOf(lines);
  }

  public int getLineCount() {
    return lines.size();
  }

  public ReviewLine getLine(int index) {
    return index >= 0 && index < lines.size() ? lines.get(index) : null;
  }

  public List<Store> getStores() {
    return stores;
  }

  public ItemSource getSource() {
    return source;
  }

  /** Date-picker initial value; uses the loaded list date or the injected clock as fallback. */
  public LocalDate initialDateFor(String value) {
    LocalDate parsed = DateText.parseIsoOrNull(value);
    if (parsed != null) {
      return parsed;
    }
    return defaultDate != null ? defaultDate : LocalDateTimes.today(clock);
  }

  public String dateLabel(String value, String fallback) {
    LocalDate parsed = DateText.parseIsoOrNull(value);
    return parsed == null ? fallback : DateText.formatMedium(parsed);
  }

  public int getSelectedCount() {
    return selectedCount;
  }

  public boolean isSaving() {
    return isSaving;
  }

  /** Add line is available in review for every capture source once its capture has loaded. */
  public boolean isAddLineAvailable() {
    return loaded && currentCapture != null;
  }

  CompletableFuture<?> inFlightAvailabilityFuture() {
    return availabilityFuture;
  }

  CompletableFuture<?> inFlightCleanFuture() {
    return cleanFuture;
  }

  public boolean isLowConfidence(ReviewLine line) {
    if (line == null || line.isManual()) {
      return false;
    }
    return RecognitionConfidence.isLow(line.getConfidence(), source);
  }

  public void loadCapture() {
    if (loaded) {
      postEvent(new ReviewUiEvent.CaptureLoaded());
      return;
    }
    if (loading) {
      return;
    }
    loading = true;
    int generation = ++loadGeneration;
    CompletableFuture.supplyAsync(
            () -> {
              CaptureSession session = captureRepository.getById(captureId);
              ShoppingList list = listRepository.getById(session.getListId());
              List<Store> activeStores = storeRepository.getActive();
              return new LoadedCapture(session, list, activeStores);
            },
            executor)
        .whenComplete(
            (loadedCapture, error) ->
                postToMain(
                    () -> {
                      if (generation != loadGeneration) {
                        return;
                      }
                      loading = false;
                      if (error != null || loadedCapture == null) {
                        postEvent(new ReviewUiEvent.LoadFailed(UiError.CAPTURE_NOT_FOUND));
                        return;
                      }
                      if (loadedCapture.stores().isEmpty()) {
                        postEvent(new ReviewUiEvent.LoadFailed(UiError.NO_STORES));
                        return;
                      }
                      currentCapture = loadedCapture.session();
                      currentList = loadedCapture.list();
                      stores = StoreOrdering.byBrandThenName(loadedCapture.stores());
                      source = currentCapture.getSource();
                      defaultDate = currentList.getShoppingDate();
                      parseLines();
                      aiCleanupEnabled = settingsRepository.isAiCleanupEnabled();
                      loaded = true;
                      refreshCleanupState();
                      checkCleanupAvailability();
                      postEvent(new ReviewUiEvent.CaptureLoaded());
                    }));
  }

  /** Reloads the persisted draft after returning from the writing screen. */
  public void reloadCapture() {
    pendingDraftWrites()
        .whenComplete(
            (ignored, error) ->
                postToMain(
                    () -> {
                      loadGeneration++;
                      loading = false;
                      loaded = false;
                      loadCapture();
                    }));
  }

  /** Returns to the writing screen with every pending review change preserved. */
  public void addLine() {
    if (draftDiscarded
        || currentCapture == null
        || NavArguments.PLACEHOLDER_CAPTURE_ID.equals(currentCapture.getId())) {
      postEvent(new ReviewUiEvent.ShowError(UiError.CAPTURE_NOT_FOUND));
      return;
    }
    String json = captureRepository.encodeDraft(toCapturedLines());
    String listId = currentCapture.getListId();
    String captureId = currentCapture.getId();
    queueDraftWrite(json, captureId)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                postEvent(new ReviewUiEvent.ShowError(UiError.SAVE_FAILED));
                return;
              }
              postEvent(new ReviewUiEvent.NavigateToInk(listId, captureId));
            });
  }

  public void addLine(String text) {
    lines.add(
        new ReviewLine(
            nextLineId++,
            text == null ? "" : text,
            null,
            false,
            0.0f,
            true,
            defaultStoreId(),
            defaultTargetDate()));
    recountSelection();
    persistDraft();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void selectAll() {
    setAllSelected(true);
    postEvent(new ReviewUiEvent.SelectionChanged());
  }

  public void deselectAll() {
    setAllSelected(false);
    postEvent(new ReviewUiEvent.SelectionChanged());
  }

  public void setLineSelected(int index, boolean selected) {
    if (index >= 0 && index < lines.size()) {
      lines.set(index, lines.get(index).withSelected(selected));
      recountSelection();
      persistDraft();
      postEvent(new ReviewUiEvent.SelectionChanged());
    }
  }

  /** Updates edited text without notifying the adapter, so the caret is not disturbed. */
  public void setLineText(int index, String text) {
    if (index >= 0 && index < lines.size()) {
      lines.set(index, lines.get(index).withText(text));
      persistDraft();
    }
  }

  public void deleteSelected() {
    lines.removeIf(ReviewLine::isSelected);
    recountSelection();
    persistDraft();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void removeLine(int index) {
    if (index >= 0 && index < lines.size()) {
      lines.remove(index);
      recountSelection();
      persistDraft();
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  /** Merges selected lines in visual order with a single space, keeping the first position. */
  public void mergeSelected() {
    int firstIndex = -1;
    List<String> parts = new ArrayList<>();
    for (int index = 0; index < lines.size(); index++) {
      ReviewLine line = lines.get(index);
      if (line.isSelected()) {
        if (firstIndex < 0) {
          firstIndex = index;
        }
        parts.add(line.getText() == null ? "" : line.getText());
      }
    }
    if (parts.size() < 2) {
      return;
    }
    ReviewLine merged = lines.get(firstIndex).withText(String.join(" ", parts));
    List<ReviewLine> updated = new ArrayList<>(lines.size());
    for (int index = 0; index < lines.size(); index++) {
      if (index == firstIndex) {
        updated.add(merged);
      } else if (!lines.get(index).isSelected()) {
        updated.add(lines.get(index));
      }
    }
    lines.clear();
    lines.addAll(updated);
    recountSelection();
    persistDraft();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  /**
   * Splits the line at the cursor position when available, otherwise at the first space. Returns
   * without changes when no split point exists.
   */
  public void splitLine(int index, int cursorPosition) {
    if (index < 0 || index >= lines.size()) {
      return;
    }
    ReviewLine line = lines.get(index);
    String text = line.getText() == null ? "" : line.getText();
    int splitAt = validCursor(cursorPosition, text) ? cursorPosition : text.indexOf(' ');
    if (splitAt <= 0 || splitAt >= text.length()) {
      postEvent(new ReviewUiEvent.ShowMessage(UiMessage.SPLIT_FAILED));
      return;
    }
    String head = text.substring(0, splitAt).trim();
    String tail = text.substring(splitAt).trim();
    if (head.isEmpty() || tail.isEmpty()) {
      postEvent(new ReviewUiEvent.ShowMessage(UiMessage.SPLIT_FAILED));
      return;
    }
    ReviewLine second =
        new ReviewLine(
            nextLineId++,
            tail,
            null,
            line.isSelected(),
            line.getConfidence(),
            line.isManual(),
            line.getStoreId(),
            line.getTargetDate());
    lines.set(index, line.withText(head));
    lines.add(index + 1, second);
    recountSelection();
    persistDraft();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void setLineStore(int index, String storeId) {
    if (index >= 0 && index < lines.size()) {
      lines.set(index, lines.get(index).withStoreId(storeId));
      persistDraft();
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  public void setLineDate(int index, LocalDate date) {
    if (index >= 0 && index < lines.size() && date != null) {
      lines.set(index, lines.get(index).withTargetDate(DateText.formatIso(date)));
      persistDraft();
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  /** Starts one cleanup run for up to five items, skipping dismissed suggestions. */
  public void startCleanup() {
    if (!aiCleanupEnabled
        || cleanupStatus == CleanupStatus.CHECKING
        || cleanupStatus == CleanupStatus.RUNNING) {
      return;
    }
    List<Integer> candidates = cleanupCandidates();
    if (candidates.isEmpty()) {
      cleanupStatus = CleanupStatus.FINISHED;
      refreshCleanupState();
      postEvent(new ReviewUiEvent.ShowMessage(UiMessage.NO_CLEANUP_CANDIDATES));
      return;
    }
    cleanupSuggestions.clear();
    cleanupStatus = CleanupStatus.CHECKING;
    refreshCleanupState();
    final int generation = ++cleanupGeneration;
    CompletableFuture<AiAvailability> availability;
    if (cleanupAvailability != null) {
      availability = CompletableFuture.completedFuture(cleanupAvailability);
    } else if (availabilityFuture != null && !availabilityFuture.isDone()) {
      availability = availabilityFuture;
    } else {
      availability = cleanerCoordinator.availability();
    }
    if (availability == null) {
      availability = CompletableFuture.completedFuture(new AiAvailability.Unavailable());
    }
    availabilityFuture = availability;
    availability.whenComplete(
        (result, error) ->
            postToMain(
                () -> {
                  if (generation != cleanupGeneration || !screenStarted) {
                    return;
                  }
                  cleanupAvailability =
                      error == null && result != null ? result : new AiAvailability.Unavailable();
                  cleanupStatus = CleanupStatus.RUNNING;
                  refreshCleanupState();
                  cleanSequentially(candidates, 0, cleanupAvailability, generation);
                }));
  }

  /** Stops further cleanup work; already found suggestions stay visible. */
  public void cancelCleanup() {
    cleanupGeneration++;
    availabilityGeneration++;
    cancelFuture(availabilityFuture);
    cancelFuture(cleanFuture);
    availabilityFuture = null;
    cleanFuture = null;
    if (cleanupStatus == CleanupStatus.CHECKING || cleanupStatus == CleanupStatus.RUNNING) {
      cleanupStatus = CleanupStatus.FINISHED;
    }
    refreshCleanupState();
  }

  public void onScreenStopped() {
    screenStarted = false;
    cancelCleanup();
    cleanerCoordinator.close();
    cleanupAvailability = null;
  }

  public void onScreenStarted() {
    screenStarted = true;
    aiCleanupEnabled = settingsRepository.isAiCleanupEnabled();
    refreshCleanupState();
    if (cleanupAvailability == null) {
      checkCleanupAvailability();
    }
  }

  public void acceptSuggestion(CleanupSuggestion suggestion) {
    acceptSuggestionAs(suggestion, suggestion.getSuggestedName());
  }

  /** Accepts a suggestion with user-edited text. The written text is not changed. */
  public void acceptSuggestionAs(CleanupSuggestion suggestion, String editedName) {
    if (suggestion == null || editedName == null || editedName.isBlank()) {
      return;
    }
    int index = suggestion.getLineIndex();
    ReviewLine line = getLine(index);
    if (line != null && Objects.equals(line.getText(), suggestion.getRawText())) {
      lines.set(index, line.withDisplayName(editedName.trim()));
      persistDraft();
      postEvent(new ReviewUiEvent.LinesChanged());
    }
    synchronized (cleanupSuggestions) {
      cleanupSuggestions.remove(suggestion);
    }
    refreshCleanupState();
  }

  public void dismissSuggestion(CleanupSuggestion suggestion) {
    if (suggestion == null) {
      return;
    }
    dismissedCleanupTexts.add(suggestion.getRawText());
    synchronized (cleanupSuggestions) {
      cleanupSuggestions.remove(suggestion);
    }
    refreshCleanupState();
  }

  public void acceptAllSuggestions() {
    boolean changed = false;
    List<CleanupSuggestion> pending;
    synchronized (cleanupSuggestions) {
      pending = List.copyOf(cleanupSuggestions);
    }
    for (CleanupSuggestion suggestion : pending) {
      int index = suggestion.getLineIndex();
      ReviewLine line = getLine(index);
      if (line != null && Objects.equals(line.getText(), suggestion.getRawText())) {
        lines.set(index, line.withDisplayName(suggestion.getSuggestedName()));
        changed = true;
      }
    }
    synchronized (cleanupSuggestions) {
      cleanupSuggestions.clear();
    }
    refreshCleanupState();
    if (changed) {
      persistDraft();
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  private List<Integer> cleanupCandidates() {
    List<Integer> candidates = new ArrayList<>(MAX_CLEANUP_ITEMS);
    for (int index = 0; index < lines.size() && candidates.size() < MAX_CLEANUP_ITEMS; index++) {
      ReviewLine line = lines.get(index);
      String text = line.getText();
      if (text == null || text.isBlank() || text.trim().length() > MAX_TEXT_LENGTH) {
        continue;
      }
      if (line.getDisplayName() != null) {
        continue;
      }
      if (dismissedCleanupTexts.contains(text)) {
        continue;
      }
      candidates.add(index);
    }
    return candidates;
  }

  private void cleanSequentially(
      List<Integer> candidates, int position, AiAvailability availability, int generation) {
    if (generation != cleanupGeneration || !screenStarted) {
      return;
    }
    if (position >= candidates.size()) {
      cleanupStatus = CleanupStatus.FINISHED;
      refreshCleanupState();
      return;
    }
    int index = candidates.get(position);
    ReviewLine line = getLine(index);
    if (line == null
        || line.getText() == null
        || line.getText().isBlank()
        || line.getDisplayName() != null
        || dismissedCleanupTexts.contains(line.getText())) {
      cleanSequentially(candidates, position + 1, availability, generation);
      return;
    }
    String rawText = line.getText();
    CleanItemRequest request = new CleanItemRequest(rawText.trim(), contextFor(index));
    CompletableFuture<CleanupOutcome> future = cleanerCoordinator.clean(request, availability);
    cleanFuture = future;
    future.whenComplete(
        (outcome, error) ->
            postToMain(
                () -> {
                  if (generation == cleanupGeneration
                      && screenStarted
                      && error == null
                      && outcome != null) {
                    ReviewLine current = getLine(index);
                    if (current != null && Objects.equals(current.getText(), rawText)) {
                      addSuggestion(index, rawText, outcome);
                    }
                  }
                  cleanSequentially(candidates, position + 1, availability, generation);
                }));
  }

  private void addSuggestion(int index, String rawText, CleanupOutcome outcome) {
    if (!(outcome.result() instanceof CleanItemResult.Suggested suggested)) {
      return;
    }
    synchronized (cleanupSuggestions) {
      cleanupSuggestions.add(
          new CleanupSuggestion(
              index,
              rawText,
              suggested.displayName(),
              suggested.confidence(),
              suggested.reason(),
              outcome.source()));
    }
    refreshCleanupState();
  }

  private List<String> contextFor(int index) {
    List<String> context = new ArrayList<>(MAX_CLEANUP_CONTEXT);
    for (int other = 0; other < lines.size() && context.size() < MAX_CLEANUP_CONTEXT; other++) {
      if (other == index) {
        continue;
      }
      String text = lines.get(other).getText();
      if (text != null && !text.isBlank()) {
        context.add(text.trim());
      }
    }
    return context;
  }

  private void checkCleanupAvailability() {
    if (!screenStarted) {
      return;
    }
    if (availabilityFuture != null && !availabilityFuture.isDone()) {
      return;
    }
    final int generation = ++availabilityGeneration;
    CompletableFuture<AiAvailability> future = cleanerCoordinator.availability();
    if (future == null) {
      cleanupAvailability = new AiAvailability.Unavailable();
      refreshCleanupState();
      return;
    }
    availabilityFuture = future;
    future.whenComplete(
        (result, error) ->
            postToMain(
                () -> {
                  if (generation != availabilityGeneration) {
                    return;
                  }
                  cleanupAvailability =
                      error == null && result != null ? result : new AiAvailability.Unavailable();
                  refreshCleanupState();
                }));
  }

  private static void cancelFuture(CompletableFuture<?> future) {
    if (future != null && !future.isDone()) {
      future.cancel(true);
    }
  }

  private void postToMain(Runnable action) {
    mainThread.execute(action);
  }

  private CleanupState currentCleanupState() {
    synchronized (cleanupSuggestions) {
      return new CleanupState(
          cleanupStatus, aiCleanupEnabled, cleanupAvailability, cleanupSuggestions);
    }
  }

  private void refreshCleanupState() {
    cleanupState.postValue(currentCleanupState());
  }

  public void saveSelected() {
    if (isSaving || currentCapture == null) {
      return;
    }
    boolean removedBlank =
        lines.removeIf(line -> line.getText() == null || line.getText().isBlank());
    if (removedBlank) {
      recountSelection();
      postEvent(new ReviewUiEvent.LinesChanged());
    }
    List<ReviewLine> toSave = new ArrayList<>();
    for (ReviewLine line : lines) {
      if (line.isSelected()) {
        toSave.add(line);
      }
    }
    if (toSave.isEmpty()) {
      postEvent(new ReviewUiEvent.ShowError(UiError.NO_LINES_SELECTED));
      return;
    }
    for (ReviewLine line : toSave) {
      if (line.getText().trim().length() > MAX_TEXT_LENGTH) {
        postEvent(new ReviewUiEvent.ShowError(UiError.RAW_TEXT_TOO_LONG));
        return;
      }
      if (line.getDisplayName() != null
          && line.getDisplayName().trim().length() > MAX_TEXT_LENGTH) {
        postEvent(new ReviewUiEvent.ShowError(UiError.RAW_TEXT_TOO_LONG));
        return;
      }
      if (line.getStoreId() == null || line.getStoreId().isBlank()) {
        postEvent(new ReviewUiEvent.ShowError(UiError.NO_STORES));
        return;
      }
    }

    isSaving = true;
    String captureIdToComplete = currentCapture.getId();
    String listId = currentCapture.getListId();
    ItemSource itemSource = currentCapture.getSource();
    String imagePath = currentCapture.getImageUri();

    List<ReviewedItem> items = new ArrayList<>(toSave.size());
    for (ReviewLine line : toSave) {
      LocalDate targetDate = DateText.parseIsoOrNull(line.getTargetDate());
      if (targetDate == null) {
        targetDate = defaultDate;
      }
      items.add(
          new ReviewedItem(
              line.getText().trim(), line.getDisplayName(), line.getStoreId(), targetDate));
    }

    saveReviewedItemsUseCase
        .save(captureIdToComplete, listId, itemSource, items)
        .thenRunAsync(() -> imageStore.delete(imagePath), executor)
        .whenComplete(
            (ignored, error) -> {
              isSaving = false;
              if (error != null) {
                postEvent(new ReviewUiEvent.ShowError(UiError.SAVE_FAILED));
                return;
              }
              if (listId != null && organizingScheduler != null) {
                organizingScheduler.scheduleAfterSave(listId);
              }
              postEvent(new ReviewUiEvent.SaveCompleted());
            });
  }

  public void discard() {
    CaptureSession session = currentCapture;
    draftDiscarded = true;
    pendingDraftWrites()
        .whenCompleteAsync(
            (ignored, writeError) -> {
              if (session != null) {
                captureRepository.softDelete(session.getId());
                imageStore.delete(session.getImageUri());
              }
            },
            executor)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                draftDiscarded = false;
                postEvent(new ReviewUiEvent.ShowError(UiError.DISCARD_FAILED));
                return;
              }
              lines.clear();
              selectedCount = 0;
              postEvent(new ReviewUiEvent.Discarded());
            });
  }

  private void parseLines() {
    lines.clear();
    String json = currentCapture.getRecognizedLinesJson();
    List<CapturedLine> draft = captureRepository.decodeDraft(json);
    if (!draft.isEmpty()) {
      for (CapturedLine line : draft) {
        if (line.getText().isBlank()) {
          continue;
        }
        lines.add(
            new ReviewLine(
                nextLineId++,
                line.getText(),
                line.getDisplayName(),
                line.isSelected(),
                line.getConfidence(),
                line.isManual(),
                line.getStoreId() == null ? defaultStoreId() : line.getStoreId(),
                line.getTargetDate() == null ? defaultTargetDate() : line.getTargetDate()));
      }
    } else {
      List<RecognizedLine> recognized = captureRepository.decodeRecognizedLines(json);
      for (RecognizedLine recognizedLine : recognized) {
        if (recognizedLine.getText().isBlank()) {
          continue;
        }
        lines.add(
            new ReviewLine(
                nextLineId++,
                recognizedLine.getText(),
                null,
                true,
                recognizedLine.getConfidence(),
                false,
                defaultStoreId(),
                defaultTargetDate()));
      }
    }
    if (lines.isEmpty()) {
      lines.add(
          new ReviewLine(
              nextLineId++, "", null, true, 0.0f, true, defaultStoreId(), defaultTargetDate()));
    }
    recountSelection();
  }

  private List<CapturedLine> toCapturedLines() {
    List<CapturedLine> draft = new ArrayList<>(lines.size());
    for (ReviewLine line : lines) {
      draft.add(
          new CapturedLine(
              line.getText(),
              line.getDisplayName(),
              line.isSelected(),
              line.getConfidence(),
              line.isManual(),
              line.getStoreId(),
              line.getTargetDate()));
    }
    return draft;
  }

  /** Keeps the review edits available when the user returns to the writing screen. */
  private void persistDraft() {
    if (draftDiscarded
        || currentCapture == null
        || NavArguments.PLACEHOLDER_CAPTURE_ID.equals(currentCapture.getId())) {
      return;
    }
    queueDraftWrite(captureRepository.encodeDraft(toCapturedLines()), currentCapture.getId())
        .exceptionally(
            error -> {
              postEvent(new ReviewUiEvent.ShowError(UiError.SAVE_FAILED));
              return null;
            });
  }

  /** Serializes draft snapshots so a slower earlier edit cannot overwrite a later one. */
  private synchronized CompletableFuture<Void> queueDraftWrite(String json, String captureId) {
    CompletableFuture<Void> write =
        draftWrites.thenRunAsync(
            () -> {
              if (!draftDiscarded) {
                captureRepository.updateDraft(captureId, json, null);
              }
            },
            executor);
    draftWrites = write.handle((ignored, error) -> null);
    return write;
  }

  private synchronized CompletableFuture<Void> pendingDraftWrites() {
    return draftWrites;
  }

  private String defaultStoreId() {
    return stores.isEmpty() ? null : stores.get(0).getId();
  }

  private String defaultTargetDate() {
    return defaultDate == null ? null : DateText.formatIso(defaultDate);
  }

  private void setAllSelected(boolean selected) {
    for (int index = 0; index < lines.size(); index++) {
      lines.set(index, lines.get(index).withSelected(selected));
    }
    recountSelection();
    persistDraft();
  }

  private void recountSelection() {
    selectedCount = 0;
    for (ReviewLine line : lines) {
      if (line.isSelected()) {
        selectedCount++;
      }
    }
  }

  private static boolean validCursor(int cursor, String text) {
    return cursor > 0 && cursor < text.length();
  }

  private void postEvent(ReviewUiEvent event) {
    events.postValue(new SingleEvent<>(event));
  }

  private record LoadedCapture(CaptureSession session, ShoppingList list, List<Store> stores) {}

  @Override
  protected void onCleared() {
    cancelCleanup();
    cleanerCoordinator.close();
  }
}
