/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.fastshoppr.app.data.capture.RecognizedLinesCodec;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.entity.CaptureSessionEntity;
import com.fastshoppr.app.data.repository.mappers.CaptureSessionMapper;
import com.fastshoppr.app.data.repository.mappers.ListItemMapper;
import com.fastshoppr.app.domain.error.NotFoundException;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class CaptureRepositoryImpl implements CaptureRepository {

  private final FastShopprDatabase database;
  private final CaptureSessionDao captureDao;
  private final ListItemDao listItemDao;
  private final Clock clock;
  private final RecognizedLinesCodec codec;

  @Inject
  public CaptureRepositoryImpl(
      FastShopprDatabase database,
      CaptureSessionDao captureDao,
      ListItemDao listItemDao,
      Clock clock,
      RecognizedLinesCodec codec) {
    this.database = database;
    this.captureDao = captureDao;
    this.listItemDao = listItemDao;
    this.clock = clock;
    this.codec = codec;
  }

  @Override
  public LiveData<List<CaptureSession>> observeByList(String listId) {
    return Transformations.map(
        captureDao.observeByList(listId),
        entities -> entities.stream().map(CaptureSessionMapper::toDomain).toList());
  }

  @Override
  public List<CaptureSession> getByList(String listId) {
    return captureDao.getByList(listId).stream().map(CaptureSessionMapper::toDomain).toList();
  }

  @Override
  public List<CaptureSession> getByListIncludingDeleted(String listId) {
    return captureDao.getByListIncludingDeleted(listId).stream()
        .map(CaptureSessionMapper::toDomain)
        .toList();
  }

  @Override
  public CaptureSession getById(String id) {
    CaptureSessionEntity entity = captureDao.getById(id);
    if (entity == null) {
      throw new NotFoundException("Capture session not found: " + id);
    }
    return CaptureSessionMapper.toDomain(entity);
  }

  @Override
  public void insert(CaptureSession session) {
    captureDao.insert(CaptureSessionMapper.toEntity(session));
  }

  @Override
  public void complete(String id, long completedAt) {
    if (captureDao.complete(id, completedAt) == 0) {
      throw new NotFoundException("Capture session not found: " + id);
    }
  }

  @Override
  public void saveReviewed(String captureId, List<ListItem> items, long completedAt) {
    database.runInTransaction(
        () -> {
          Map<String, Integer> maxByStore = new HashMap<>();
          for (ListItem item : items) {
            String key = item.getListId() + "\0" + item.getStoreId();
            int next =
                maxByStore.computeIfAbsent(
                        key,
                        ignored -> {
                          Integer max =
                              listItemDao.getMaxSortPosition(item.getListId(), item.getStoreId());
                          return max == null ? -1 : max;
                        })
                    + 1;
            maxByStore.put(key, next);
            listItemDao.insert(
                ListItemMapper.toEntity(ListItem.builderFrom(item).sortPosition(next).build()));
          }
          if (captureDao.complete(captureId, completedAt) == 0) {
            throw new NotFoundException("Capture session not found: " + captureId);
          }
        });
  }

  @Override
  public void softDelete(String id) {
    if (captureDao.softDelete(id, clock.nowMillis()) == 0) {
      throw new NotFoundException("Capture session not found: " + id);
    }
  }

  @Override
  public int purgeDeletedBefore(long cutoffMillis) {
    return captureDao.purgeDeletedBefore(cutoffMillis);
  }

  @Override
  public String encodeRecognizedLines(List<RecognizedLine> lines) {
    return codec.encode(lines);
  }

  @Override
  public List<RecognizedLine> decodeRecognizedLines(String json) {
    return codec.decode(json);
  }
}
