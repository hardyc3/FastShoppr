/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import com.fastshoppr.app.domain.text.TextNormalizer;
import java.util.Objects;

public final class CaptureSession {

  private final String id;
  private final String listId;
  private final ItemSource source;
  private final String imageUri;
  private final String recognizedLinesJson;
  private final long createdAt;
  private final Long completedAt;
  private final Long deletedAt;

  public CaptureSession(
      String id,
      String listId,
      ItemSource source,
      String imageUri,
      String recognizedLinesJson,
      long createdAt,
      Long completedAt,
      Long deletedAt) {
    this.id = requireId(id, "capture id");
    this.listId = requireId(listId, "list id");
    this.source = Objects.requireNonNull(source, "source must not be null");
    this.imageUri = TextNormalizer.trimToNull(imageUri);
    if (recognizedLinesJson == null) {
      throw new IllegalArgumentException("recognizedLinesJson must not be null");
    }
    this.recognizedLinesJson = recognizedLinesJson;
    this.createdAt = requireTimestamp(createdAt, "createdAt");
    this.completedAt = validateTimestamp(completedAt, "completedAt");
    this.deletedAt = validateTimestamp(deletedAt, "deletedAt");
  }

  private static String requireId(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static long requireTimestamp(long value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
    return value;
  }

  private static Long validateTimestamp(Long value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
    return value;
  }

  public String getId() {
    return id;
  }

  public String getListId() {
    return listId;
  }

  public ItemSource getSource() {
    return source;
  }

  public String getImageUri() {
    return imageUri;
  }

  public String getRecognizedLinesJson() {
    return recognizedLinesJson;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public Long getCompletedAt() {
    return completedAt;
  }

  public Long getDeletedAt() {
    return deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
