/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.ArchiveListUseCase;
import com.fastshoppr.app.domain.usecase.CreateListUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public final class ListsViewModel extends ViewModel {

  private static final String KEY_SHOW_ARCHIVED = "show_archived";

  private final ShoppingListRepository listRepository;
  private final CreateListUseCase createListUseCase;
  private final ArchiveListUseCase archiveListUseCase;
  private final Clock clock;
  private final SavedStateHandle savedStateHandle;

  private final MutableLiveData<ListsUiState> state = new MutableLiveData<>(ListsUiState.loading());
  private final MutableLiveData<SingleEvent<ListsUiEvent>> events = new MutableLiveData<>();
  private final LiveData<List<ShoppingListSummary>> activeLiveData;
  private final LiveData<List<ShoppingListSummary>> archivedLiveData;

  private final Observer<List<ShoppingListSummary>> activeObserver = this::onActiveChanged;
  private final Observer<List<ShoppingListSummary>> archivedObserver = this::onArchivedChanged;

  private List<ShoppingListSummary> active = List.of();
  private List<ShoppingListSummary> archived = List.of();
  private boolean activeLoaded;
  private boolean archivedLoaded;

  @Inject
  public ListsViewModel(
      ShoppingListRepository listRepository,
      CreateListUseCase createListUseCase,
      ArchiveListUseCase archiveListUseCase,
      Clock clock,
      SavedStateHandle savedStateHandle) {
    this.listRepository = listRepository;
    this.createListUseCase = createListUseCase;
    this.archiveListUseCase = archiveListUseCase;
    this.clock = clock;
    this.savedStateHandle = savedStateHandle;
    this.activeLiveData = listRepository.observeActiveSummaries();
    this.archivedLiveData = listRepository.observeArchivedSummaries();
    activeLiveData.observeForever(activeObserver);
    archivedLiveData.observeForever(archivedObserver);
  }

  public LiveData<ListsUiState> getState() {
    return state;
  }

  public LiveData<SingleEvent<ListsUiEvent>> getEvents() {
    return events;
  }

  public void toggleShowArchived() {
    boolean showArchived = !isShowingArchived();
    savedStateHandle.set(KEY_SHOW_ARCHIVED, showArchived);
    rebuild();
  }

  public void createList(LocalDate shoppingDate, String listName) {
    createListUseCase
        .create(shoppingDate, listName, null, false)
        .whenComplete(
            (list, error) -> {
              if (error != null) {
                events.postValue(
                    new SingleEvent<>(new ListsUiEvent.ShowError(UiError.CREATE_FAILED)));
                return;
              }
              events.postValue(new SingleEvent<>(new ListsUiEvent.OpenList(list.getId())));
            });
  }

  public void archiveList(String listId) {
    archiveListUseCase
        .archive(listId)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                events.postValue(
                    new SingleEvent<>(new ListsUiEvent.ShowError(UiError.ARCHIVE_FAILED)));
                return;
              }
              events.postValue(
                  new SingleEvent<>(new ListsUiEvent.ShowMessage(UiMessage.LIST_ARCHIVED)));
            });
  }

  private void onActiveChanged(List<ShoppingListSummary> summaries) {
    active = summaries == null ? List.of() : summaries;
    activeLoaded = true;
    rebuild();
  }

  private void onArchivedChanged(List<ShoppingListSummary> summaries) {
    archived = summaries == null ? List.of() : summaries;
    archivedLoaded = true;
    rebuild();
  }

  private void rebuild() {
    if (!activeLoaded || !archivedLoaded) {
      state.setValue(ListsUiState.loading());
      return;
    }

    LocalDate today =
        Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
    List<ShoppingListSummary> upcoming = new ArrayList<>();
    List<ShoppingListSummary> past = new ArrayList<>();
    for (ShoppingListSummary summary : active) {
      if (summary.getList().getShoppingDate().isBefore(today)) {
        past.add(summary);
      } else {
        upcoming.add(summary);
      }
    }

    boolean showArchived = isShowingArchived();
    List<ListRow> rows = new ArrayList<>();
    if (!upcoming.isEmpty()) {
      rows.add(new ListRow.SectionHeader(ListRow.Section.UPCOMING));
      upcoming.forEach(summary -> rows.add(new ListRow.ListEntry(summary)));
    }
    if (!past.isEmpty()) {
      rows.add(new ListRow.SectionHeader(ListRow.Section.PAST));
      past.forEach(summary -> rows.add(new ListRow.ListEntry(summary)));
    }
    if (showArchived && !archived.isEmpty()) {
      rows.add(new ListRow.SectionHeader(ListRow.Section.ARCHIVED));
      archived.forEach(summary -> rows.add(new ListRow.ListEntry(summary)));
    }

    state.setValue(new ListsUiState(false, rows, showArchived, rows.isEmpty(), null));
  }

  private boolean isShowingArchived() {
    return Boolean.TRUE.equals(savedStateHandle.get(KEY_SHOW_ARCHIVED));
  }

  @Override
  protected void onCleared() {
    activeLiveData.removeObserver(activeObserver);
    archivedLiveData.removeObserver(archivedObserver);
  }
}
