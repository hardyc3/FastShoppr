/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.fastshoppr.app.data.local.dao.AisleCacheDao;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.dao.ShoppingScheduleDao;
import com.fastshoppr.app.data.local.dao.StoreDao;
import com.fastshoppr.app.data.local.dao.UserDataDao;
import com.fastshoppr.app.data.local.entity.AisleCacheEntity;
import com.fastshoppr.app.data.local.entity.CaptureSessionEntity;
import com.fastshoppr.app.data.local.entity.ListItemEntity;
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.data.local.entity.ShoppingScheduleEntity;
import com.fastshoppr.app.data.local.entity.StoreEntity;

@Database(
    entities = {
      StoreEntity.class,
      ShoppingListEntity.class,
      ListItemEntity.class,
      CaptureSessionEntity.class,
      ShoppingScheduleEntity.class,
      AisleCacheEntity.class
    },
    version = 6,
    exportSchema = true)
public abstract class FastShopprDatabase extends RoomDatabase {

  public static final String NAME = "fastshoppr.db";

  public abstract StoreDao storeDao();

  public abstract ShoppingListDao shoppingListDao();

  public abstract ListItemDao listItemDao();

  public abstract CaptureSessionDao captureSessionDao();

  public abstract ShoppingScheduleDao shoppingScheduleDao();

  public abstract AisleCacheDao aisleCacheDao();

  public abstract UserDataDao userDataDao();
}
