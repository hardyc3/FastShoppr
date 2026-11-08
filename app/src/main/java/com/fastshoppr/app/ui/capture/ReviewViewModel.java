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
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class ReviewViewModel extends ViewModel {

  public sealed interface ReviewUiEvent
      permits ReviewUiEvent.LineAdded,
          ReviewUiEvent.LinesChanged,
          ReviewUiEvent.SelectionChanged,
          ReviewUiEvent.SaveCompleted,
          ReviewUiEvent.Discarded {

    record LineAdded() implements ReviewUiEvent {}

    record LinesChanged() implements ReviewUiEvent {}

    record SelectionChanged() implements ReviewUiEvent {}

    record SaveCompleted() implements ReviewUiEvent {}

    record Discarded() implements ReviewUiEvent {}
  }

  public static final class ReviewLine {
    public String text;
    public boolean selected;
    public float confidence;
    public String storeId;
    public String targetDate;

    public ReviewLine(String text, boolean selected, float confidence) {
      this.text = text;
      this.selected = selected;
      this.confidence = confidence;
      this.storeId = null;
      this.targetDate = null;
    }
  }

  private final CaptureRepository captureRepository;
  private final ListItemRepository listItemRepository;
  private final Clock clock;
  private final ExecutorService executor;
  private final SavedStateHandle savedStateHandle;

  private final MutableLiveData<SingleEvent<ReviewUiEvent>> events = new MutableLiveData<>();

  private final String captureId;
  private CaptureSession currentCapture;
  private final List<ReviewLine> lines = new ArrayList<>();
  private int selectedCount = 0;
  private boolean isSaving = false;

  @Inject
  public ReviewViewModel(
      CaptureRepository captureRepository,
      ListItemRepository listItemRepository,
      Clock clock,
      ExecutorService executor,
      SavedStateHandle savedStateHandle) {
    this.captureRepository = captureRepository;
    this.listItemRepository = listItemRepository;
    this.clock = clock;
    this.executor = executor;
    this.savedStateHandle = savedStateHandle;
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

  public int getSelectedCount() {
    return selectedCount;
  }

  public boolean isSaving() {
    return isSaving;
  }

  public void loadCapture() {
    currentCapture = captureRepository.getById(captureId);
    if (currentCapture != null) {
      parseLines();
    }
  }

  public void addLine(String text) {
    lines.add(new ReviewLine(text, false, 0.0f));
    postEvent(new ReviewUiEvent.LineAdded());
  }

  public void removeLine(int index) {
    if (index >= 0 && index < lines.size()) {
      lines.remove(index);
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  public void selectAll() {
    for (ReviewLine line : lines) {
      line.selected = true;
    }
    selectedCount = lines.size();
    postEvent(new ReviewUiEvent.SelectionChanged());
  }

  public void deselectAll() {
    for (ReviewLine line : lines) {
      line.selected = false;
    }
    selectedCount = 0;
    postEvent(new ReviewUiEvent.SelectionChanged());
  }

  public void deleteSelected() {
    List<Integer> indices = new ArrayList<>();
    for (int index = lines.size() - 1; index >= 0; index--) {
      if (lines.get(index).selected) {
        indices.add(index);
      }
    }
    for (int index : indices) {
      lines.remove(index);
    }
    selectedCount = 0;
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  public void mergeSelected() {
    List<String> selectedTexts = new ArrayList<>();
    for (ReviewLine line : lines) {
      if (line.selected) {
        selectedTexts.add(line.text);
      }
    }
    if (selectedTexts.size() >= 2) {
      deleteSelected();
      addLine(String.join(" ", selectedTexts));
    }
  }

  public void splitLine(int index) {
    ReviewLine line = lines.get(index);
    String[] parts = line.text.split("\\s+");
    if (parts.length >= 2) {
      lines.set(index, new ReviewLine(parts[0], line.selected, line.confidence));
      lines.add(index + 1, new ReviewLine(parts[1], line.selected, line.confidence));
      postEvent(new ReviewUiEvent.LinesChanged());
    }
  }

  public void saveSelected() {
    if (isSaving || currentCapture == null) {
      return;
    }
    isSaving = true;
    List<ReviewLine> selectedLines = new ArrayList<>();
    for (ReviewLine line : lines) {
      if (line.selected) {
        selectedLines.add(line);
      }
    }
    List<CompletableFuture<?>> pending = new ArrayList<>(selectedLines.size());
    for (ReviewLine line : selectedLines) {
      String storeId = line.storeId == null ? "" : line.storeId.trim();
      LocalDate targetDate = parseTargetDate(line.targetDate);
      if (storeId.isEmpty() || targetDate == null) {
        continue;
      }
      final String finalStoreId = storeId;
      final String finalText = line.text == null ? "" : line.text;
      final LocalDate finalTargetDate = targetDate;
      final String finalListId = currentCapture.getListId();
      pending.add(
          CompletableFuture.supplyAsync(
              () -> {
                ListItem item =
                    ListItem.builder()
                        .id(UUID.randomUUID().toString())
                        .listId(finalListId)
                        .storeId(finalStoreId)
                        .rawText(finalText)
                        .displayName(finalText)
                        .normalizedText(normalizeText(finalText))
                        .targetDate(finalTargetDate)
                        .source(ItemSource.CAPTURE)
                        .isChecked(false)
                        .sortPosition(
                            listItemRepository.nextSortPosition(finalListId, finalStoreId))
                        .build();
                listItemRepository.insert(item);
                return item;
              },
              executor));
    }
    CompletableFuture.allOf(pending.toArray(new CompletableFuture[0]))
        .whenComplete(
            (ignored, error) -> {
              if (error == null && currentCapture != null) {
                captureRepository.complete(currentCapture.getId(), clock.nowMillis());
              }
              events.postValue(new SingleEvent<>(new ReviewUiEvent.SaveCompleted()));
              isSaving = false;
            });
  }

  public void discard() {
    if (currentCapture != null) {
      captureRepository.softDelete(currentCapture.getId());
    }
    lines.clear();
    selectedCount = 0;
    postEvent(new ReviewUiEvent.Discarded());
  }

  private void parseLines() {
    lines.clear();
    lines.add(new ReviewLine("", true, 1.0f));
    postEvent(new ReviewUiEvent.LinesChanged());
  }

  private LocalDate parseTargetDate(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private String normalizeText(String text) {
    return text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
  }

  private void postEvent(ReviewUiEvent event) {
    events.postValue(new SingleEvent<>(event));
  }
}
