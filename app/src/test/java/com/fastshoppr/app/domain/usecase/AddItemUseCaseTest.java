/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import java.time.LocalDate;
import org.junit.Test;

public final class AddItemUseCaseTest {

  @Test
  public void persistsItemWithDerivedTextAndDate() throws Exception {
    ShoppingListRepository listRepository = mock(ShoppingListRepository.class);
    ListItemRepository itemRepository = mock(ListItemRepository.class);
    ShoppingList list =
        new ShoppingList(
            "list-1", "Weekly shop", LocalDate.of(2026, 11, 7), null, false, false, null, 1, 1);
    when(listRepository.getById("list-1")).thenReturn(list);
    when(itemRepository.nextSortPosition("list-1", "store-1")).thenReturn(4);

    ListItem item =
        new AddItemUseCase(
                listRepository,
                itemRepository,
                new FixedIdGenerator(),
                new FixedClock(500),
                new DirectExecutorService())
            .add("list-1", "store-1", "  Paper   Towels! ", null, "2", ItemSource.TYPED)
            .get();

    assertEquals("id-1", item.getId());
    assertEquals("  Paper   Towels! ".trim(), item.getRawText());
    assertEquals("Paper Towels!", item.getDisplayName());
    assertEquals("paper towels", item.getNormalizedText());
    assertEquals(LocalDate.of(2026, 11, 7), item.getTargetDate());
    assertEquals("store-1", item.getStoreId());
    assertEquals("2", item.getQuantity());
    assertEquals(4, item.getSortPosition());
    assertEquals(LookupStatus.PENDING, item.getLookupStatus());
    assertEquals(ItemSource.TYPED, item.getSource());
    assertEquals(500, item.getCreatedAt());
    verify(itemRepository).insert(item);
  }
}
