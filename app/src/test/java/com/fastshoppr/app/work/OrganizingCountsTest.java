/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

public final class OrganizingCountsTest {

  @Test
  public void countsStatusesAndSummarizes() {
    OrganizingCounts counts =
        OrganizingCounts.from(
            List.of(
                item("1", LookupStatus.FOUND),
                item("2", LookupStatus.FOUND),
                item("3", LookupStatus.NEEDS_REVIEW),
                item("4", LookupStatus.NOT_FOUND),
                item("5", LookupStatus.ERROR),
                item("6", LookupStatus.PENDING),
                item("7", LookupStatus.QUEUED),
                item("8", LookupStatus.IN_PROGRESS)));

    assertEquals(2, counts.found);
    assertEquals(1, counts.needsReview);
    assertEquals(1, counts.notFound);
    assertEquals(1, counts.errors);
    assertEquals(3, counts.pending);
    assertEquals("Found 2, needs review 1, not found 1, errors 1", counts.summary());
  }

  @Test
  public void emptyAndNullsAreSafe() {
    OrganizingCounts empty = OrganizingCounts.from(List.of());
    assertEquals(0, empty.found);
    assertEquals(0, empty.pending);
    OrganizingCounts nil = OrganizingCounts.from(null);
    assertEquals("Found 0, needs review 0, not found 0, errors 0", nil.summary());
  }

  private static ListItem item(String id, LookupStatus status) {
    return ListItem.builder()
        .id(id)
        .listId("list-1")
        .storeId("store-1")
        .rawText("item")
        .displayName("item")
        .normalizedText("item")
        .targetDate(LocalDate.of(2026, 11, 15))
        .lookupStatus(status)
        .source(ItemSource.TYPED)
        .sortPosition(0)
        .createdAt(1L)
        .updatedAt(1L)
        .build();
  }
}
