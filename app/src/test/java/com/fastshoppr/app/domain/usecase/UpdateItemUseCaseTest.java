/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import java.time.LocalDate;
import org.junit.Test;

public final class UpdateItemUseCaseTest {

  @Test
  public void updatesManualAisleWithoutReplacingRawText() throws Exception {
    ListItemRepository repository = mock(ListItemRepository.class);
    ListItem existing =
        ListItem.builder()
            .id("item-1")
            .listId("list-1")
            .storeId("store-1")
            .rawText("milk")
            .displayName("milk")
            .normalizedText("milk")
            .targetDate(LocalDate.of(2026, 11, 7))
            .sortPosition(2)
            .createdAt(1)
            .updatedAt(1)
            .build();
    when(repository.getById("item-1")).thenReturn(existing);

    ListItem updated =
        new UpdateItemUseCase(repository, new FixedClock(900), new DirectExecutorService())
            .update("item-1", "milk", "Whole milk", "1 gal", "store-1", "A12", "3")
            .get();

    assertEquals("milk", updated.getRawText());
    assertEquals("Whole milk", updated.getDisplayName());
    assertEquals("whole milk", updated.getNormalizedText());
    assertEquals("A12", updated.getAisle());
    assertEquals("3", updated.getBay());
    assertEquals(LookupStatus.FOUND, updated.getLookupStatus());
    assertEquals(LookupSource.MANUAL, updated.getLookupSource());
    assertEquals(900, updated.getUpdatedAt());
    verify(repository).update(updated);
  }

  @Test
  public void clearsManualLocationWhenAisleIsRemoved() throws Exception {
    ListItemRepository repository = mock(ListItemRepository.class);
    ListItem existing =
        ListItem.builder()
            .id("item-1")
            .listId("list-1")
            .storeId("store-1")
            .rawText("milk")
            .targetDate(LocalDate.of(2026, 11, 7))
            .aisle("A12")
            .lookupStatus(LookupStatus.FOUND)
            .lookupSource(LookupSource.MANUAL)
            .createdAt(1)
            .updatedAt(1)
            .build();
    when(repository.getById("item-1")).thenReturn(existing);

    ListItem updated =
        new UpdateItemUseCase(repository, new FixedClock(900), new DirectExecutorService())
            .update("item-1", "milk", "milk", null, "store-1", null, null)
            .get();

    assertEquals(LookupStatus.PENDING, updated.getLookupStatus());
    assertNull(updated.getLookupSource());
  }
}
