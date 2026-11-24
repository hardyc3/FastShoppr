/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.data.capture.CapturedLinesCodec;
import com.fastshoppr.app.data.capture.RecognizedLinesCodec;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.entity.ListItemEntity;
import com.fastshoppr.app.domain.error.NotFoundException;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.time.Clock;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import org.mockito.InOrder;

public final class CaptureRepositoryImplTest {

  private static final RecognizedLinesCodec CODEC = new RecognizedLinesCodec(new Gson());
  private static final CapturedLinesCodec DRAFT_CODEC = new CapturedLinesCodec(new Gson());

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
    CaptureRepositoryImpl txRepository = repository(database, captureDao, listItemDao);

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
            1000L,
            null);

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
    CaptureRepositoryImpl txRepository = repository(database, captureDao, listItemDao);

    ListItem a = reviewedItem("i1", "store-1", "a");
    ListItem b = reviewedItem("i2", "store-1", "b");
    txRepository.saveReviewed("cap-1", List.of(a, b), 1000L);

    org.mockito.ArgumentCaptor<ListItemEntity> captor =
        org.mockito.ArgumentCaptor.forClass(ListItemEntity.class);
    org.mockito.Mockito.verify(listItemDao, org.mockito.Mockito.times(2)).insert(captor.capture());
    assertEquals(5, captor.getAllValues().get(0).sortPosition);
    assertEquals(6, captor.getAllValues().get(1).sortPosition);
  }

  @Test
  public void draftJsonIsEncodedAndDecodedWithCapturedLinesCodec() {
    CaptureRepositoryImpl repository =
        repository(
            mock(FastShopprDatabase.class), mock(CaptureSessionDao.class), mock(ListItemDao.class));
    CapturedLine line =
        new CapturedLine("milk", "Organic milk", false, 0.9f, true, "store-1", "2026-11-08");

    String json = repository.encodeDraft(List.of(line));
    List<CapturedLine> decoded = repository.decodeDraft(json);

    assertTrue(json.contains("\"displayName\":\"Organic milk\""));
    assertEquals(1, decoded.size());
    assertEquals("milk", decoded.get(0).getText());
    assertEquals("Organic milk", decoded.get(0).getDisplayName());
    assertEquals(false, decoded.get(0).isSelected());
    assertEquals("store-1", decoded.get(0).getStoreId());
    assertEquals("2026-11-08", decoded.get(0).getTargetDate());
  }

  @Test
  public void malformedDraftDecodesToEmptyList() {
    CaptureRepositoryImpl repository =
        repository(
            mock(FastShopprDatabase.class), mock(CaptureSessionDao.class), mock(ListItemDao.class));

    assertTrue(repository.decodeDraft("{not json").isEmpty());
    assertTrue(repository.decodeDraft(null).isEmpty());
  }

  @Test
  public void updateDraftPersistsJsonForExistingSession() {
    CaptureSessionDao captureDao = mock(CaptureSessionDao.class);
    when(captureDao.updateDraft("cap-1", "draft-json", null)).thenReturn(1);
    CaptureRepositoryImpl repository =
        repository(mock(FastShopprDatabase.class), captureDao, mock(ListItemDao.class));

    repository.updateDraft("cap-1", "draft-json", null);

    verify(captureDao).updateDraft("cap-1", "draft-json", null);
  }

  @Test
  public void updateDraftThrowsWhenSessionIsMissing() {
    CaptureSessionDao captureDao = mock(CaptureSessionDao.class);
    when(captureDao.updateDraft("missing", "draft-json", null)).thenReturn(0);
    CaptureRepositoryImpl repository =
        repository(mock(FastShopprDatabase.class), captureDao, mock(ListItemDao.class));

    assertThrows(
        NotFoundException.class, () -> repository.updateDraft("missing", "draft-json", null));
  }

  private static CaptureRepositoryImpl repository(
      FastShopprDatabase database, CaptureSessionDao captureDao, ListItemDao listItemDao) {
    return new CaptureRepositoryImpl(
        database, captureDao, listItemDao, mock(Clock.class), CODEC, DRAFT_CODEC);
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
        1000L,
        null);
  }
}
