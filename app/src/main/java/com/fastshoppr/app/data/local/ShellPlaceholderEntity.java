/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shell_placeholder")
public final class ShellPlaceholderEntity {

  @PrimaryKey @NonNull public final String id;

  public ShellPlaceholderEntity(@NonNull String id) {
    this.id = id;
  }
}
