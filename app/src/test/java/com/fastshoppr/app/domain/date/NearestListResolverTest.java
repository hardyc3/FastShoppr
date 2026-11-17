/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import java.time.LocalDate;
import org.junit.Test;

public final class NearestListResolverTest {

  @Test
  public void returnsExistingNearestListOnOrAfterTarget() {
    ShoppingListRepository repo = mock(ShoppingListRepository.class);
    ShoppingList list =
        new ShoppingList("l1", "List", LocalDate.of(2026, 11, 8), null, false, false, null, 1, 1);
    when(repo.findNearestActiveOnOrAfter(LocalDate.of(2026, 11, 5))).thenReturn(list);
    NearestListResolver.Resolution r =
        new NearestListResolver(repo).resolve(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 2));
    assertTrue(r.existingList().isPresent());
    assertFalse(r.createList());
    assertEquals(LocalDate.of(2026, 11, 8), r.existingList().get().getShoppingDate());
  }

  @Test
  public void requestsCreateWhenNoListExists() {
    ShoppingListRepository repo = mock(ShoppingListRepository.class);
    when(repo.findNearestActiveOnOrAfter(LocalDate.of(2026, 11, 9))).thenReturn(null);
    NearestListResolver.Resolution r =
        new NearestListResolver(repo).resolve(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 2));
    assertFalse(r.existingList().isPresent());
    assertTrue(r.createList());
  }

  @Test
  public void flagsPastTargetForConfirmation() {
    ShoppingListRepository repo = mock(ShoppingListRepository.class);
    when(repo.findNearestActiveOnOrAfter(LocalDate.of(2026, 10, 30))).thenReturn(null);
    NearestListResolver.Resolution r =
        new NearestListResolver(repo)
            .resolve(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2));
    assertTrue(r.needsConfirmation());
  }
}
