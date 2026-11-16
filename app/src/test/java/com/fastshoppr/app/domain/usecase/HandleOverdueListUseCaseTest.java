/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import java.time.LocalDate;
import org.junit.Test;

public final class HandleOverdueListUseCaseTest {

  @Test
  public void missedArchivesPastList() throws Exception {
    ShoppingListRepository listRepo = mock(ShoppingListRepository.class);
    ListItemRepository itemRepo = mock(ListItemRepository.class);
    HandleOverdueListUseCase useCase = new HandleOverdueListUseCase(
        listRepo, itemRepo, new FixedClock(0), new DirectExecutorService());

    useCase.apply(HandleOverdueListUseCase.Action.MISSED, "past", null, null).get();

    verify(listRepo).archive("past");
  }

  @Test
  public void moveUncheckedMovesToTargetList() throws Exception {
    ShoppingListRepository listRepo = mock(ShoppingListRepository.class);
    ListItemRepository itemRepo = mock(ListItemRepository.class);
    ShoppingList target = new ShoppingList("t1", "Next", LocalDate.of(2026, 11, 10), null, false, false, null, 1, 1);
    when(listRepo.getById("t1")).thenReturn(target);
    HandleOverdueListUseCase useCase = new HandleOverdueListUseCase(
        listRepo, itemRepo, new FixedClock(0), new DirectExecutorService());

    useCase.apply(HandleOverdueListUseCase.Action.MOVE_UNCHECKED, "past", "t1", null).get();

    verify(itemRepo).moveUncheckedItemsToList(eq("past"), eq("t1"), eq(LocalDate.of(2026, 11, 10)));
  }

  @Test
  public void moveUncheckedCreatesTargetWhenMissing() throws Exception {
    ShoppingListRepository listRepo = mock(ShoppingListRepository.class);
    ListItemRepository itemRepo = mock(ListItemRepository.class);
    ShoppingList created = new ShoppingList("c1", "Shopping list 2026-11-11", LocalDate.of(2026, 11, 11), null, false, false, null, 1, 1);
    when(listRepo.getOrCreate(LocalDate.of(2026, 11, 11), "Shopping list 2026-11-11", null, false)).thenReturn(created);
    HandleOverdueListUseCase useCase = new HandleOverdueListUseCase(
        listRepo, itemRepo, new FixedClock(0), new DirectExecutorService());

    useCase.apply(HandleOverdueListUseCase.Action.MOVE_UNCHECKED, "past", null, LocalDate.of(2026, 11, 11)).get();

    verify(itemRepo).moveUncheckedItemsToList(eq("past"), eq("c1"), eq(LocalDate.of(2026, 11, 11)));
  }
}
