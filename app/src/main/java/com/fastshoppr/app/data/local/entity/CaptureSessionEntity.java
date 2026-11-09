/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.fastshoppr.app.domain.model.ItemSource;

@Entity(
    tableName = "capture_sessions",
    foreignKeys =
        @ForeignKey(
            entity = ShoppingListEntity.class,
            parentColumns = "id",
            childColumns = "listId",
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("listId"), @Index("deletedAt")})
public final class CaptureSessionEntity {

  @PrimaryKey @NonNull public final String id;

  @NonNull public final String listId;

  @NonNull public final ItemSource source;

  @Nullable public final String imageUri;

  @NonNull public final String recognizedLinesJson;

  public final long createdAt;

  @Nullable public final Long completedAt;

  @Nullable public final Long deletedAt;

  public CaptureSessionEntity(
      @NonNull String id,
      @NonNull String listId,
      @NonNull ItemSource source,
      @Nullable String imageUri,
      @NonNull String recognizedLinesJson,
      long createdAt,
      @Nullable Long completedAt,
      @Nullable Long deletedAt) {
    this.id = id;
    this.listId = listId;
    this.source = source;
    this.imageUri = imageUri;
    this.recognizedLinesJson = recognizedLinesJson;
    this.createdAt = createdAt;
    this.completedAt = completedAt;
    this.deletedAt = deletedAt;
  }
}
