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
import androidx.room.Update;
import com.fastshoppr.app.data.local.entity.ListItemEntity;
import java.util.List;

@Dao
public interface ListItemDao {

  @Query(
      "SELECT * FROM list_items WHERE listId = :listId AND deletedAt IS NULL "
          + "ORDER BY sortPosition ASC")
  LiveData<List<ListItemEntity>> observeByList(String listId);

  @Query(
      "SELECT * FROM list_items WHERE listId = :listId AND storeId = :storeId "
          + "AND deletedAt IS NULL ORDER BY sortPosition ASC")
  LiveData<List<ListItemEntity>> observeByListAndStore(String listId, String storeId);

  @Query(
      "SELECT * FROM list_items WHERE listId = :listId AND deletedAt IS NULL "
          + "ORDER BY sortPosition ASC")
  List<ListItemEntity> getByList(String listId);

  @Query("SELECT * FROM list_items WHERE listId = :listId")
  List<ListItemEntity> getByListIncludingDeleted(String listId);

  @Query("SELECT * FROM list_items WHERE id = :id AND deletedAt IS NULL")
  ListItemEntity getById(String id);

  @Query("SELECT * FROM list_items WHERE id = :id")
  ListItemEntity getByIdIncludingDeleted(String id);

  @Query(
      "SELECT MAX(sortPosition) FROM list_items "
          + "WHERE listId = :listId AND storeId = :storeId AND deletedAt IS NULL")
  Integer getMaxSortPosition(String listId, String storeId);

  @Insert
  void insert(ListItemEntity item);

  @Update
  int update(ListItemEntity item);

  @Query(
      "UPDATE list_items SET isChecked = :checked, updatedAt = :updatedAt "
          + "WHERE id = :id AND deletedAt IS NULL")
  int setChecked(String id, boolean checked, long updatedAt);

  @Query(
      "UPDATE list_items SET listId = :listId, targetDate = :targetDate, "
          + "sortPosition = :sortPosition, updatedAt = :updatedAt "
          + "WHERE id = :id AND deletedAt IS NULL")
  int moveToDate(String id, String listId, String targetDate, int sortPosition, long updatedAt);

  @Query(
      "UPDATE list_items SET deletedAt = :deletedAt, updatedAt = :deletedAt "
          + "WHERE id = :id AND deletedAt IS NULL")
  int softDelete(String id, long deletedAt);

  @Query(
      "UPDATE list_items SET deletedAt = :deletedAt, updatedAt = :deletedAt "
          + "WHERE listId = :listId AND deletedAt IS NULL")
  int softDeleteByList(String listId, long deletedAt);

  @Query(
      "UPDATE list_items SET deletedAt = NULL, updatedAt = :updatedAt "
          + "WHERE listId = :listId AND deletedAt = :deletedAt")
  int restoreByListDeletedAt(String listId, long deletedAt, long updatedAt);

  @Query("DELETE FROM list_items WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
  int purgeDeletedBefore(long cutoff);
}
