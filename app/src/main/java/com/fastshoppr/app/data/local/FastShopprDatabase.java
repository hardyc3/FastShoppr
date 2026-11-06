/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
    entities = {ShellPlaceholderEntity.class},
    version = 1,
    exportSchema = true)
public abstract class FastShopprDatabase extends RoomDatabase {

  public static final String NAME = "fastshoppr.db";
}
