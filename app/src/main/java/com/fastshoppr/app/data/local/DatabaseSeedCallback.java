/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.time.Clock;

public final class DatabaseSeedCallback extends RoomDatabase.Callback {

  private static final String STORES_TABLE = "stores";

  private final Clock clock;

  public DatabaseSeedCallback(Clock clock) {
    this.clock = clock;
  }

  @Override
  public void onOpen(@NonNull SupportSQLiteDatabase database) {
    super.onOpen(database);
    long now = clock.nowMillis();
    for (StoreBrand brand : SeedStores.brands()) {
      ContentValues values = new ContentValues();
      values.put("id", SeedStores.idFor(brand));
      values.put("brand", brand.name());
      values.put("name", SeedStores.nameFor(brand));
      values.put("isCustom", 0);
      values.put("isDefault", 0);
      values.put("isArchived", 0);
      values.put("createdAt", now);
      values.put("updatedAt", now);
      database.insert(STORES_TABLE, SQLiteDatabase.CONFLICT_IGNORE, values);
    }
  }
}
