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
import com.fastshoppr.app.data.local.entity.ShoppingScheduleEntity;
import java.util.List;

@Dao
public interface ShoppingScheduleDao {

  @Query("SELECT * FROM shopping_schedules ORDER BY nextShoppingDate ASC")
  LiveData<List<ShoppingScheduleEntity>> observeAll();

  @Query("SELECT * FROM shopping_schedules ORDER BY nextShoppingDate ASC")
  List<ShoppingScheduleEntity> getAll();

  @Query(
      "SELECT * FROM shopping_schedules WHERE isEnabled = 1 "
          + "ORDER BY nextShoppingDate ASC LIMIT 1")
  ShoppingScheduleEntity getActive();

  @Query("SELECT * FROM shopping_schedules WHERE id = :id")
  ShoppingScheduleEntity getById(String id);

  @Insert
  void insert(ShoppingScheduleEntity schedule);

  @Update
  int update(ShoppingScheduleEntity schedule);

  @Query(
      "UPDATE shopping_schedules SET isEnabled = 0, updatedAt = :updatedAt "
          + "WHERE isEnabled = 1 AND id != :exceptId")
  int disableOthers(String exceptId, long updatedAt);

  @Query("DELETE FROM shopping_schedules WHERE id = :id")
  int deleteById(String id);
}
