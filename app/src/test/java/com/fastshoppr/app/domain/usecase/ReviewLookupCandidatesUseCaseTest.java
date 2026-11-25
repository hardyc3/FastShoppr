/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.lookup.LookupCandidatesCodec;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

public final class ReviewLookupCandidatesUseCaseTest {

  private final ReviewLookupCandidatesUseCase useCase = new ReviewLookupCandidatesUseCase();
  private final ReviewLookupCandidatesUseCase.Labels labels =
      new ReviewLookupCandidatesUseCase.Labels() {
        @Override
        public String unknownProduct() {
          return "Unknown product";
        }

        @Override
        public String noAisle() {
          return "No aisle";
        }

        @Override
        public String baySuffix(String bay) {
          return " · Bay " + bay;
        }
      };

  @Test
  public void needsReviewItemProducesLabeledCandidates() {
    String json =
        LookupCandidatesCodec.encode(
            List.of(
                new StoreLookupResult.Found("A12", "3", "Milk", null, 0.9f, LookupSource.CACHE)));
    ReviewLookupCandidatesUseCase.Result result =
        useCase.decode(item(LookupStatus.NEEDS_REVIEW, json), labels);

    assertTrue(result.showCandidates());
    assertEquals(1, result.candidates().size());
    assertEquals("A12", result.candidates().get(0).candidate().getAisle());
    assertEquals("Milk (A12 · Bay 3)", result.candidates().get(0).label());
  }

  @Test
  public void missingProductNameUsesUnknownLabel() {
    String json =
        LookupCandidatesCodec.encode(
            List.of(new StoreLookupResult.Found("B4", null, null, null, null, LookupSource.CACHE)));
    ReviewLookupCandidatesUseCase.Result result =
        useCase.decode(item(LookupStatus.NEEDS_REVIEW, json), labels);

    assertEquals("Unknown product (B4)", result.candidates().get(0).label());
  }

  @Test
  public void candidatesWithoutAisleAreFilteredBeforeLabeling() {
    String json =
        LookupCandidatesCodec.encode(
            List.of(
                new StoreLookupResult.Found(null, null, "Bread", null, null, LookupSource.CACHE)));
    ReviewLookupCandidatesUseCase.Result result =
        useCase.decode(item(LookupStatus.NEEDS_REVIEW, json), labels);

    assertFalse(result.showCandidates());
    assertTrue(result.candidates().isEmpty());
  }

  @Test
  public void otherStatusesAndUnreadableJsonDoNotOfferCandidates() {
    String json =
        LookupCandidatesCodec.encode(
            List.of(
                new StoreLookupResult.Found("A12", null, "Milk", null, null, LookupSource.CACHE)));

    assertFalse(useCase.decode(item(LookupStatus.FOUND, json), labels).showCandidates());
    assertFalse(
        useCase.decode(item(LookupStatus.NEEDS_REVIEW, "{broken"), labels).showCandidates());
    assertFalse(useCase.decode(null, labels).showCandidates());
  }

  private static ListItem item(LookupStatus status, String json) {
    return ListItem.builder()
        .id("item-1")
        .listId("list-1")
        .storeId("store-1")
        .rawText("milk")
        .displayName("milk")
        .normalizedText("milk")
        .targetDate(LocalDate.of(2026, 11, 24))
        .lookupStatus(status)
        .matchCandidatesJson(json)
        .source(ItemSource.TYPED)
        .sortPosition(0)
        .createdAt(1L)
        .updatedAt(1L)
        .build();
  }
}
