/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.fastshoppr.app.data.local.entity.CaptureSessionEntity;
import java.util.List;

@Dao
public interface CaptureSessionDao {

  @Query(
      "SELECT * FROM capture_sessions WHERE listId = :listId AND deletedAt IS NULL "
          + "ORDER BY createdAt ASC")
  LiveData<List<CaptureSessionEntity>> observeByList(String listId);

  @Query(
      "SELECT * FROM capture_sessions WHERE listId = :listId AND deletedAt IS NULL "
          + "ORDER BY createdAt ASC")
  List<CaptureSessionEntity> getByList(String listId);

  @Query("SELECT * FROM capture_sessions WHERE listId = :listId")
  List<CaptureSessionEntity> getByListIncludingDeleted(String listId);

  @Query("SELECT * FROM capture_sessions WHERE id = :id AND deletedAt IS NULL")
  CaptureSessionEntity getById(String id);

  @Query("SELECT * FROM capture_sessions WHERE id = :id")
  CaptureSessionEntity getByIdIncludingDeleted(String id);

  @Insert
  void insert(CaptureSessionEntity session);

  @Query(
      "UPDATE capture_sessions SET completedAt = :completedAt "
          + "WHERE id = :id AND deletedAt IS NULL")
  int complete(String id, long completedAt);

  @Query(
      "UPDATE capture_sessions SET deletedAt = :deletedAt "
          + "WHERE id = :id AND deletedAt IS NULL")
  int softDelete(String id, long deletedAt);

  @Query(
      "UPDATE capture_sessions SET deletedAt = :deletedAt "
          + "WHERE listId = :listId AND deletedAt IS NULL")
  int softDeleteByList(String listId, long deletedAt);

  @Query(
      "UPDATE capture_sessions SET deletedAt = NULL "
          + "WHERE listId = :listId AND deletedAt = :deletedAt")
  int restoreByListDeletedAt(String listId, long deletedAt);

  @Query("DELETE FROM capture_sessions WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
  int purgeDeletedBefore(long cutoff);
}
