/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.fastshoppr.app.data.local.entity.StoreEntity;
import java.util.List;

/** One-shot clear of all user data, leaving only the built-in store catalog. */
@Dao
public interface UserDataDao {

  @Transaction
  default void clearUserDataAndReseedStores(List<StoreEntity> seedStores) {
    deleteListItems();
    deleteCaptureSessions();
    deleteShoppingLists();
    deleteSchedules();
    deleteAisleCache();
    deleteStores();
    for (StoreEntity store : seedStores) {
      insertStore(store);
    }
  }

  @Query("DELETE FROM list_items")
  void deleteListItems();

  @Query("DELETE FROM capture_sessions")
  void deleteCaptureSessions();

  @Query("DELETE FROM shopping_lists")
  void deleteShoppingLists();

  @Query("DELETE FROM shopping_schedules")
  void deleteSchedules();

  @Query("DELETE FROM aisle_cache")
  void deleteAisleCache();

  @Query("DELETE FROM stores")
  void deleteStores();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertStore(StoreEntity store);
}
