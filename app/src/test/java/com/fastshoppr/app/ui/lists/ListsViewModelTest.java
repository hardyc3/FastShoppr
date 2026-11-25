/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import com.fastshoppr.app.domain.observation.Observable;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.ArchiveListUseCase;
import com.fastshoppr.app.domain.usecase.CreateListUseCase;
import com.fastshoppr.app.testing.FixedClock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public final class ListsViewModelTest {

  @Mock private ShoppingListRepository listRepository;
  @Mock private CreateListUseCase createListUseCase;
  @Mock private ArchiveListUseCase archiveListUseCase;

  private AutoCloseable mocks;

  @Before
  public void setUp() {
    mocks = openMocks(this);
  }

  @After
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void initialCreateDateUsesInjectedClock() {
    LocalDate expected = LocalDate.of(2026, 11, 24);
    long now = expected.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    Observable<List<ShoppingListSummary>> observable = observer -> () -> {};
    when(listRepository.observeActiveSummaries()).thenReturn(observable);
    when(listRepository.observeArchivedSummaries()).thenReturn(observable);
    Clock clock = new FixedClock(now);

    ListsViewModel viewModel =
        new ListsViewModel(
            listRepository, createListUseCase, archiveListUseCase, clock, new SavedStateHandle());

    assertEquals(expected, viewModel.initialCreateDate());
  }
}
