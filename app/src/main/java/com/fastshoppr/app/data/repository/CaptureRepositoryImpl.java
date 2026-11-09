/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.entity.CaptureSessionEntity;
import com.fastshoppr.app.data.repository.mappers.CaptureSessionMapper;
import com.fastshoppr.app.domain.error.NotFoundException;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class CaptureRepositoryImpl implements CaptureRepository {

  private final CaptureSessionDao captureDao;
  private final Clock clock;
  private final Gson gson;

  @Inject
  public CaptureRepositoryImpl(CaptureSessionDao captureDao, Clock clock) {
    this(captureDao, clock, new Gson());
  }

  CaptureRepositoryImpl(CaptureSessionDao captureDao, Clock clock, Gson gson) {
    this.captureDao = captureDao;
    this.clock = clock;
    this.gson = gson;
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
    return gson.toJson(lines == null ? List.of() : lines);
  }

  @Override
  public List<RecognizedLine> decodeRecognizedLines(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<RecognizedLine> lines =
          gson.fromJson(json, new TypeToken<List<RecognizedLine>>() {}.getType());
      return lines == null ? List.of() : lines;
    } catch (RuntimeException exception) {
      return List.of();
    }
  }
}
