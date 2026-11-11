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
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.RecognitionConfidence;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ReviewedItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.usecase.SaveReviewedItemsUseCase;
import com.fastshoppr.app.ui.common.DateText;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StoreOrdering;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class ReviewViewModel extends ViewModel {

  private static final int MAX_TEXT_LENGTH = 200;

  public sealed interface ReviewUiEvent
      permits ReviewUiEvent.CaptureLoaded,
          ReviewUiEvent.LoadFailed,
          ReviewUiEvent.LinesChanged,
          ReviewUiEvent.SelectionChanged,
          ReviewUiEvent.SaveCompleted,
          ReviewUiEvent.Discarded,
          ReviewUiEvent.ShowError,
          ReviewUiEvent.ShowMessage {

    record CaptureLoaded() implements ReviewUiEvent {}

    record LoadFailed(UiError error) implements ReviewUiEvent {}

    record LinesChanged() implements ReviewUiEvent {}

    record SelectionChanged() implements ReviewUiEvent {}

    record SaveCompleted() implements ReviewUiEvent {}

    record Discarded() implements ReviewUiEvent {}

    record ShowError(UiError error) implements ReviewUiEvent {}

    record ShowMessage(UiMessage message) implements ReviewUiEvent {}
  }

  public static final class ReviewLine {
    private final String text;
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
      this.text = text;
      this.selected = selected;
      this.confidence = confidence;
      this.manual = manual;
      this.storeId = storeId;
      this.targetDate = targetDate;
    }

    public String getText() {
      return text;
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
      return new ReviewLine(value, selected, confidence, manual, storeId, targetDate);
    }

    ReviewLine withSelected(boolean value) {
      return new ReviewLine(text, value, confidence, manual, storeId, targetDate);
    }

    ReviewLine withStoreId(String value) {
      return new ReviewLine(text, selected, confidence, manual, value, targetDate);
    }

    ReviewLine withTargetDate(String value) {
      return new ReviewLine(text, selected, confidence, manual, storeId, value);
    }
  }

  private final CaptureRepository captureRepository;
  private final StoreRepository storeRepository;
  private final ShoppingListRepository listRepository;
  private final CaptureImageStore imageStore;
  private final SaveReviewedItemsUseCase saveReviewedItemsUseCase;
  private final ExecutorService executor;

  private final MutableLiveData<SingleEvent<ReviewUiEvent>> events = new MutableLiveData<>();

  private final String captureId;
  private final List<ReviewLine> lines = new ArrayList<>();

  private CaptureSession currentCapture;
  private ShoppingList currentList;
  private List<Store> stores = List.of();
  private ItemSource source = ItemSource.CAPTURE;
  private LocalDate defaultDate;
  private int selectedCount;
  private boolean isSaving;
  private boolean loading;
  private boolean loaded;

  @Inject
  public ReviewViewModel(
      CaptureRepository captureRepository,
      StoreRepository storeRepository,
      ShoppingListRepository listRepository,
      CaptureImageStore imageStore,
      SaveReviewedItemsUseCase saveReviewedItemsUseCase,
      ExecutorService executor,
      SavedStateHandle savedStateHandle) {
    this.captureRepository = captureRepository;
    this.storeRepository = storeRepository;
    this.listRepository = listRepository;
    this.imageStore = imageStore;
    this.saveReviewedItemsUseCase = saveReviewedItemsUseCase;
    this.executor = executor;
    this.captureId = savedStateHandle.get(NavArguments.CAPTURE_ID);
  }

  public LiveData<SingleEvent<ReviewUiEvent>> getEvents() {
    return events;
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

  public int getSelectedCount() {
    return selectedCount;
  }

  public boolean isSaving() {
    return isSaving;
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
    CompletableFuture.supplyAsync(
            () -> {
              CaptureSession session = captureRepository.getById(captureId);
              ShoppingList list = listRepository.getById(session.getListId());
              List<Store> activeStores = storeRepository.getActive();
              return new LoadedCapture(session, list, activeStores);
            },
            executor)
        .whenComplete(
            (loadedCapture, error) -> {
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
              loaded = true;
              postEvent(new ReviewUiEvent.CaptureLoaded());
            });
  }

  public void addLine(String text) {
    lines.add(
        new ReviewLine(
            text == null ? "" : text, false, 0.0f, true, defaultStoreId(), defaultTargetDate()));
    recountSelection();
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
      postEvent(new ReviewUiEvent.SelectionChanged());
    }
  }

  /** Updates edited text without notifying the adapter, so the caret is not disturbed. */
  public void setLineText(int index, String text) {
    if (index >= 0 && index < lines.size()) {
      lines.set(index, lines.get(index).withText(text));
    }
  }

  public void deleteSelected() {
    lines.removeIf(ReviewLine::isSelected);
    recountSelection();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void removeLine(int index) {
    if (index >= 0 && index < lines.size()) {
      lines.remove(index);
      recountSelection();
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
            tail,
            line.isSelected(),
            line.getConfidence(),
            line.isManual(),
            line.getStoreId(),
            line.getTargetDate());
    lines.set(index, line.withText(head));
    lines.add(index + 1, second);
    recountSelection();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void setLineStore(int index, String storeId) {
    if (index >= 0 && index < lines.size()) {
      lines.set(index, lines.get(index).withStoreId(storeId));
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  public void setLineDate(int index, LocalDate date) {
    if (index >= 0 && index < lines.size() && date != null) {
      lines.set(index, lines.get(index).withTargetDate(DateText.formatIso(date)));
      postEvent(new ReviewUiEvent.LinesChanged());
    }
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
      items.add(new ReviewedItem(line.getText().trim(), line.getStoreId(), targetDate));
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
              postEvent(new ReviewUiEvent.SaveCompleted());
            });
  }

  public void discard() {
    CaptureSession session = currentCapture;
    CompletableFuture.runAsync(
            () -> {
              if (session != null) {
                captureRepository.softDelete(session.getId());
                imageStore.delete(session.getImageUri());
              }
            },
            executor)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
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
    List<RecognizedLine> recognized =
        captureRepository.decodeRecognizedLines(currentCapture.getRecognizedLinesJson());
    for (RecognizedLine recognizedLine : recognized) {
      if (recognizedLine.getText().isBlank()) {
        continue;
      }
      ReviewLine line =
          new ReviewLine(
              recognizedLine.getText(),
              true,
              recognizedLine.getConfidence(),
              false,
              defaultStoreId(),
              defaultTargetDate());
      lines.add(line);
    }
    if (lines.isEmpty()) {
      lines.add(new ReviewLine("", true, 0.0f, true, defaultStoreId(), defaultTargetDate()));
    }
    recountSelection();
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
}
