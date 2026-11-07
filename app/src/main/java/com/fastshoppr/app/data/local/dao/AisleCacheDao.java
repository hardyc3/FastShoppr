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
import com.fastshoppr.app.data.local.entity.AisleCacheEntity;

@Dao
public interface AisleCacheDao {

  @Query(
      "SELECT * FROM aisle_cache WHERE storeId = :storeId "
          + "AND normalizedQuery = :normalizedQuery LIMIT 1")
  AisleCacheEntity find(String storeId, String normalizedQuery);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsert(AisleCacheEntity cache);

  @Query("DELETE FROM aisle_cache WHERE id = :id")
  int deleteById(String id);

  @Query("DELETE FROM aisle_cache WHERE storeId = :storeId")
  int deleteByStore(String storeId);

  @Query("DELETE FROM aisle_cache WHERE expiresAt < :nowMillis")
  int deleteExpired(long nowMillis);
}
