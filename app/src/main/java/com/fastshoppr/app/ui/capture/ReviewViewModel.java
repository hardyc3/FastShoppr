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
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognitionConfidence;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    public String text;
    public boolean selected;
    public float confidence;
    public boolean manual;
    public String storeId;
    public String targetDate;

    public ReviewLine(String text, boolean selected, float confidence) {
      this.text = text;
      this.selected = selected;
      this.confidence = confidence;
      this.manual = false;
      this.storeId = null;
      this.targetDate = null;
    }
  }

  private final CaptureRepository captureRepository;
  private final ListItemRepository listItemRepository;
  private final StoreRepository storeRepository;
  private final ShoppingListRepository listRepository;
  private final CaptureImageStore imageStore;
  private final IdGenerator idGenerator;
  private final Clock clock;
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
      ListItemRepository listItemRepository,
      StoreRepository storeRepository,
      ShoppingListRepository listRepository,
      CaptureImageStore imageStore,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor,
      SavedStateHandle savedStateHandle) {
    this.captureRepository = captureRepository;
    this.listItemRepository = listItemRepository;
    this.storeRepository = storeRepository;
    this.listRepository = listRepository;
    this.imageStore = imageStore;
    this.idGenerator = idGenerator;
    this.clock = clock;
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
    return lines;
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
    if (line == null || line.manual) {
      return false;
    }
    return RecognitionConfidence.isLow(line.confidence, source);
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
              stores = orderStores(loadedCapture.stores());
              source = currentCapture.getSource();
              defaultDate = currentList.getShoppingDate();
              parseLines();
              loaded = true;
              postEvent(new ReviewUiEvent.CaptureLoaded());
            });
  }

  public void addLine(String text) {
    ReviewLine line = new ReviewLine(text == null ? "" : text, false, 0.0f);
    line.manual = true;
    line.storeId = defaultStoreId();
    line.targetDate = defaultTargetDate();
    lines.add(line);
    recountSelection();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void selectAll() {
    for (ReviewLine line : lines) {
      line.selected = true;
    }
    recountSelection();
    postEvent(new ReviewUiEvent.SelectionChanged());
  }

  public void deselectAll() {
    for (ReviewLine line : lines) {
      line.selected = false;
    }
    recountSelection();
    postEvent(new ReviewUiEvent.SelectionChanged());
  }

  public void setLineSelected(int index, boolean selected) {
    if (index >= 0 && index < lines.size()) {
      lines.get(index).selected = selected;
      recountSelection();
      postEvent(new ReviewUiEvent.SelectionChanged());
    }
  }

  public void deleteSelected() {
    lines.removeIf(line -> line.selected);
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
    for (ReviewLine line : lines) {
      if (line.selected) {
        if (firstIndex < 0) {
          firstIndex = lines.indexOf(line);
        }
        parts.add(line.text == null ? "" : line.text);
      }
    }
    if (parts.size() < 2) {
      return;
    }
    ReviewLine first = lines.get(firstIndex);
    first.text = String.join(" ", parts);
    for (int index = lines.size() - 1; index > firstIndex; index--) {
      if (lines.get(index).selected) {
        lines.remove(index);
      }
    }
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
    String text = line.text == null ? "" : line.text;
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
    ReviewLine second = new ReviewLine(tail, line.selected, line.confidence);
    second.manual = line.manual;
    second.storeId = line.storeId;
    second.targetDate = line.targetDate;
    line.text = head;
    lines.add(index + 1, second);
    recountSelection();
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void setLineStore(int index, String storeId) {
    if (index >= 0 && index < lines.size()) {
      lines.get(index).storeId = storeId;
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  public void setLineDate(int index, LocalDate date) {
    if (index >= 0 && index < lines.size() && date != null) {
      lines.get(index).targetDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  public void saveSelected() {
    if (isSaving || currentCapture == null) {
      return;
    }
    boolean removedBlank = lines.removeIf(line -> line.text == null || line.text.isBlank());
    if (removedBlank) {
      recountSelection();
      postEvent(new ReviewUiEvent.LinesChanged());
    }
    List<ReviewLine> toSave = new ArrayList<>();
    for (ReviewLine line : lines) {
      if (line.selected) {
        toSave.add(line);
      }
    }
    if (toSave.isEmpty()) {
      postEvent(new ReviewUiEvent.ShowError(UiError.NO_LINES_SELECTED));
      return;
    }
    for (ReviewLine line : toSave) {
      if (line.text.trim().length() > MAX_TEXT_LENGTH) {
        isSaving = false;
        postEvent(new ReviewUiEvent.ShowError(UiError.RAW_TEXT_TOO_LONG));
        return;
      }
      if (line.storeId == null || line.storeId.isBlank()) {
        postEvent(new ReviewUiEvent.ShowError(UiError.NO_STORES));
        return;
      }
    }

    isSaving = true;
    String captureIdToComplete = currentCapture.getId();
    String listId = currentCapture.getListId();
    ItemSource itemSource = currentCapture.getSource();
    String imagePath = currentCapture.getImageUri();
    long now = clock.nowMillis();

    CompletableFuture<Void> save =
        CompletableFuture.runAsync(
            () -> {
              for (ReviewLine line : toSave) {
                LocalDate targetDate = parseTargetDate(line.targetDate);
                if (targetDate == null) {
                  targetDate = defaultDate;
                }
                ListItem item =
                    ListItem.builder()
                        .id(idGenerator.newId())
                        .listId(listId)
                        .storeId(line.storeId)
                        .rawText(line.text.trim())
                        .displayName(null)
                        .targetDate(targetDate)
                        .source(itemSource)
                        .isChecked(false)
                        .sortPosition(
                            listItemRepository.nextSortPosition(listId, line.storeId))
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                listItemRepository.insert(item);
              }
              captureRepository.complete(captureIdToComplete, now);
              imageStore.delete(imagePath);
            },
            executor);
    save.whenComplete(
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
    executor.execute(
        () -> {
          if (session != null) {
            try {
              captureRepository.softDelete(session.getId());
              imageStore.delete(session.getImageUri());
            } catch (RuntimeException exception) {
              return;
            }
          }
        });
    lines.clear();
    selectedCount = 0;
    postEvent(new ReviewUiEvent.Discarded());
  }

  private void parseLines() {
    lines.clear();
    List<RecognizedLine> recognized = captureRepository.decodeRecognizedLines(
        currentCapture.getRecognizedLinesJson());
    for (RecognizedLine recognizedLine : recognized) {
      if (recognizedLine.getText().isBlank()) {
        continue;
      }
      ReviewLine line = new ReviewLine(recognizedLine.getText(), true, recognizedLine.getConfidence());
      line.storeId = defaultStoreId();
      line.targetDate = defaultTargetDate();
      lines.add(line);
    }
    if (lines.isEmpty()) {
      ReviewLine empty = new ReviewLine("", true, 0.0f);
      empty.manual = true;
      empty.storeId = defaultStoreId();
      empty.targetDate = defaultTargetDate();
      lines.add(empty);
    }
    recountSelection();
  }

  private String defaultStoreId() {
    return stores.isEmpty() ? null : stores.get(0).getId();
  }

  private String defaultTargetDate() {
    return defaultDate == null ? null : defaultDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  private void recountSelection() {
    selectedCount = 0;
    for (ReviewLine line : lines) {
      if (line.selected) {
        selectedCount++;
      }
    }
  }

  private static boolean validCursor(int cursor, String text) {
    return cursor > 0 && cursor < text.length();
  }

  private static LocalDate parseTargetDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private static List<Store> orderStores(List<Store> activeStores) {
    List<Store> ordered = new ArrayList<>(activeStores);
    ordered.sort(
        Comparator.comparingInt((Store store) -> store.getBrand().ordinal())
            .thenComparing(Store::getName, String.CASE_INSENSITIVE_ORDER));
    return ordered;
  }

  private void postEvent(ReviewUiEvent event) {
    events.postValue(new SingleEvent<>(event));
  }

  private record LoadedCapture(CaptureSession session, ShoppingList list, List<Store> stores) {}
}
