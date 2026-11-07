/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.fastshoppr.app.domain.model.CaptureSession;
import java.util.List;

public interface CaptureRepository {

  LiveData<List<CaptureSession>> observeByList(String listId);

  List<CaptureSession> getByList(String listId);

  List<CaptureSession> getByListIncludingDeleted(String listId);

  CaptureSession getById(String id);

  void insert(CaptureSession session);

  void complete(String id, long completedAt);

  void softDelete(String id);

  int purgeDeletedBefore(long cutoffMillis);
}
