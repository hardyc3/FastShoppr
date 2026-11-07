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
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import java.util.List;

@Dao
public interface ShoppingListDao {

  @Query(
      "SELECT * FROM shopping_lists "
          + "WHERE isArchived = 0 AND deletedAt IS NULL ORDER BY shoppingDate ASC")
  LiveData<List<ShoppingListEntity>> observeActive();

  @Query(
      "SELECT * FROM shopping_lists "
          + "WHERE isArchived = 0 AND deletedAt IS NULL ORDER BY shoppingDate ASC")
  List<ShoppingListEntity> getActive();

  @Query(
      "SELECT * FROM shopping_lists "
          + "WHERE isArchived = 1 AND deletedAt IS NULL ORDER BY shoppingDate DESC")
  LiveData<List<ShoppingListEntity>> observeArchived();

  @Query("SELECT * FROM shopping_lists WHERE id = :id AND deletedAt IS NULL")
  ShoppingListEntity getById(String id);

  @Query("SELECT * FROM shopping_lists WHERE id = :id")
  ShoppingListEntity getByIdIncludingDeleted(String id);

  @Query(
      "SELECT * FROM shopping_lists "
          + "WHERE shoppingDate = :shoppingDate AND isArchived = 0 AND deletedAt IS NULL LIMIT 1")
  ShoppingListEntity findActiveByDate(String shoppingDate);

  @Query(
      "SELECT COUNT(*) FROM shopping_lists "
          + "WHERE shoppingDate = :shoppingDate AND isArchived = 0 AND deletedAt IS NULL")
  int countActiveByDate(String shoppingDate);

  @Insert
  void insert(ShoppingListEntity list);

  @Update
  int update(ShoppingListEntity list);

  @Query(
      "UPDATE shopping_lists SET isArchived = :archived, updatedAt = :updatedAt "
          + "WHERE id = :id AND deletedAt IS NULL")
  int setArchived(String id, boolean archived, long updatedAt);

  @Query(
      "UPDATE shopping_lists SET deletedAt = :deletedAt, updatedAt = :deletedAt "
          + "WHERE id = :id AND deletedAt IS NULL")
  int softDelete(String id, long deletedAt);

  @Query(
      "UPDATE shopping_lists SET deletedAt = NULL, updatedAt = :updatedAt "
          + "WHERE id = :id AND deletedAt = :deletedAt")
  int restore(String id, long deletedAt, long updatedAt);

  @Query("DELETE FROM shopping_lists WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
  int purgeDeletedBefore(long cutoff);
}
