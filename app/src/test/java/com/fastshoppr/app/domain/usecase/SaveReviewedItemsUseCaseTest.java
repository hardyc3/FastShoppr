/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ReviewedItem;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public final class SaveReviewedItemsUseCaseTest {

  @Test
  @SuppressWarnings("unchecked")
  public void mapsReviewedLinesToItemsAndCompletesCapture() throws Exception {
    CaptureRepository repository = mock(CaptureRepository.class);
    SaveReviewedItemsUseCase useCase =
        new SaveReviewedItemsUseCase(
            repository, new FixedIdGenerator(), new FixedClock(1000L), new DirectExecutorService());

    useCase
        .save(
            "cap-1",
            "list-1",
            ItemSource.DIGITAL_INK,
            List.of(new ReviewedItem("milk", "store-1", LocalDate.of(2026, 11, 8))))
        .get();

    ArgumentCaptor<List<ListItem>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveReviewed(eq("cap-1"), captor.capture(), eq(1000L));
    ListItem item = captor.getValue().get(0);
    assertEquals("id-1", item.getId());
    assertEquals("milk", item.getRawText());
    assertEquals("milk", item.getDisplayName());
    assertEquals("store-1", item.getStoreId());
    assertEquals(ItemSource.DIGITAL_INK, item.getSource());
    assertEquals(LocalDate.of(2026, 11, 8), item.getTargetDate());
    assertEquals(1000L, item.getCreatedAt());
  }
}
