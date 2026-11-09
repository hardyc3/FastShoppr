/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.entity.ListItemEntity;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.time.Clock;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import org.mockito.InOrder;

public final class CaptureRepositoryImplTest {

  private final CaptureRepositoryImpl repository = new CaptureRepositoryImpl(new Gson());

  @Test
  public void recognizedLinesRoundTripPreservesTextAndConfidence() {
    List<RecognizedLine> lines =
        List.of(new RecognizedLine("milk", 0.95f), new RecognizedLine("eggs", 0.2f));

    String json = repository.encodeRecognizedLines(lines);
    List<RecognizedLine> decoded = repository.decodeRecognizedLines(json);

    assertEquals(2, decoded.size());
    assertEquals("milk", decoded.get(0).getText());
    assertEquals(0.95f, decoded.get(0).getConfidence(), 0.0001f);
    assertEquals("eggs", decoded.get(1).getText());
    assertEquals(0.2f, decoded.get(1).getConfidence(), 0.0001f);
  }

  @Test
  public void encodeNullOrEmptyYieldsEmptyArray() {
    assertEquals("[]", repository.encodeRecognizedLines(null));
    assertEquals("[]", repository.encodeRecognizedLines(List.of()));
  }

  @Test
  public void decodeMalformedOrNullYieldsEmptyList() {
    assertTrue(repository.decodeRecognizedLines(null).isEmpty());
    assertTrue(repository.decodeRecognizedLines("").isEmpty());
    assertTrue(repository.decodeRecognizedLines("{not json").isEmpty());
    assertTrue(repository.decodeRecognizedLines("[{\"bogus\":").isEmpty());
  }

  @Test(expected = IllegalStateException.class)
  public void saveReviewedRequiresDatabaseAndDaos() {
    repository.saveReviewed("cap-1", List.of(), 1000L);
  }

  @Test
  public void saveReviewedInsertsItemsThenCompletesAtomically() {
    FastShopprDatabase database = mock(FastShopprDatabase.class);
    CaptureSessionDao captureDao = mock(CaptureSessionDao.class);
    ListItemDao listItemDao = mock(ListItemDao.class);
    doAnswer(
            invocation -> {
              Runnable body = invocation.getArgument(0);
              body.run();
              return null;
            })
        .when(database)
        .runInTransaction(any(Runnable.class));
    when(listItemDao.getMaxSortPosition("list-1", "store-1")).thenReturn(2);
    when(captureDao.complete("cap-1", 1000L)).thenReturn(1);
    CaptureRepositoryImpl txRepository =
        new CaptureRepositoryImpl(database, captureDao, listItemDao, mock(Clock.class));

    ListItem item =
        new ListItem(
            "item-1",
            "list-1",
            "store-1",
            "milk",
            null,
            null,
            null,
            LocalDate.of(2026, 11, 8),
            null,
            null,
            null,
            null,
            null,
            com.fastshoppr.app.domain.model.LookupStatus.PENDING,
            null,
            null,
            ItemSource.CAMERA_OCR,
            false,
            0,
            null,
            1000L,
            1000L);

    txRepository.saveReviewed("cap-1", List.of(item), 1000L);

    InOrder order = inOrder(listItemDao, captureDao);
    order.verify(listItemDao).insert(any(ListItemEntity.class));
    order.verify(captureDao).complete("cap-1", 1000L);
  }

  @Test
  public void saveReviewedComputesContiguousSortPositionsPerStore() {
    FastShopprDatabase database = mock(FastShopprDatabase.class);
    CaptureSessionDao captureDao = mock(CaptureSessionDao.class);
    ListItemDao listItemDao = mock(ListItemDao.class);
    doAnswer(
            invocation -> {
              ((Runnable) invocation.getArgument(0)).run();
              return null;
            })
        .when(database)
        .runInTransaction(any(Runnable.class));
    when(listItemDao.getMaxSortPosition("list-1", "store-1")).thenReturn(4);
    when(captureDao.complete("cap-1", 1000L)).thenReturn(1);
    CaptureRepositoryImpl txRepository =
        new CaptureRepositoryImpl(database, captureDao, listItemDao, mock(Clock.class));

    ListItem a = reviewedItem("i1", "store-1", "a");
    ListItem b = reviewedItem("i2", "store-1", "b");
    txRepository.saveReviewed("cap-1", List.of(a, b), 1000L);

    org.mockito.ArgumentCaptor<ListItemEntity> captor =
        org.mockito.ArgumentCaptor.forClass(ListItemEntity.class);
    org.mockito.Mockito.verify(listItemDao, org.mockito.Mockito.times(2)).insert(captor.capture());
    assertEquals(5, captor.getAllValues().get(0).sortPosition);
    assertEquals(6, captor.getAllValues().get(1).sortPosition);
  }

  private static ListItem reviewedItem(String id, String storeId, String text) {
    return new ListItem(
        id,
        "list-1",
        storeId,
        text,
        null,
        null,
        null,
        LocalDate.of(2026, 11, 8),
        null,
        null,
        null,
        null,
        null,
        com.fastshoppr.app.domain.model.LookupStatus.PENDING,
        null,
        null,
        ItemSource.CAMERA_OCR,
        false,
        0,
        null,
        1000L,
        1000L);
  }
}
