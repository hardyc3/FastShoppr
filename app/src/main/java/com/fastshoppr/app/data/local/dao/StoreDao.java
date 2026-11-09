/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.fastshoppr.app.data.local.entity.StoreEntity;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.List;

@Dao
public interface StoreDao {

  @Query("SELECT * FROM stores WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
  LiveData<List<StoreEntity>> observeActive();

  @Query("SELECT * FROM stores WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
  List<StoreEntity> getActive();

  @Query("SELECT * FROM stores WHERE id = :id")
  StoreEntity getById(String id);

  @Query("SELECT * FROM stores WHERE brand = :brand AND isArchived = 0 LIMIT 1")
  StoreEntity findActiveByBrand(StoreBrand brand);

  @Query("SELECT * FROM stores WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
  StoreEntity getDefault();

  @Query("SELECT COUNT(*) FROM list_items WHERE storeId = :storeId")
  int countItemReferences(String storeId);

  @Query("SELECT COUNT(*) FROM aisle_cache WHERE storeId = :storeId")
  int countCacheReferences(String storeId);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsert(StoreEntity store);

  @Query("UPDATE stores SET isDefault = 0, updatedAt = :updatedAt WHERE id != :storeId")
  void clearDefaultExcept(String storeId, long updatedAt);

  @Query(
      "UPDATE stores SET isDefault = 1, updatedAt = :updatedAt "
          + "WHERE id = :storeId AND isArchived = 0")
  int setDefault(String storeId, long updatedAt);

  @Query("UPDATE stores SET isArchived = 1, updatedAt = :updatedAt WHERE id = :storeId")
  int archive(String storeId, long updatedAt);

  @Query("UPDATE stores SET isArchived = 0, updatedAt = :updatedAt WHERE id = :storeId")
  int restore(String storeId, long updatedAt);

  @Query("DELETE FROM stores WHERE id = :id")
  int deleteById(String id);
}
