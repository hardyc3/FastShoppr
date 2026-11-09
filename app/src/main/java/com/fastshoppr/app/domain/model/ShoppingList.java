/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import com.fastshoppr.app.domain.text.TextNormalizer;
import java.time.LocalDate;
import java.util.Objects;

public final class ShoppingList {

  private static final int MAX_NAME_LENGTH = 80;

  private final String id;
  private final String name;
  private final LocalDate shoppingDate;
  private final String scheduleId;
  private final boolean isAutoCreated;
  private final boolean isArchived;
  private final Long deletedAt;
  private final long createdAt;
  private final long updatedAt;

  public ShoppingList(
      String id,
      String name,
      LocalDate shoppingDate,
      String scheduleId,
      boolean isAutoCreated,
      boolean isArchived,
      Long deletedAt,
      long createdAt,
      long updatedAt) {
    this.id = requireId(id, "list id");
    this.name = validateName(name);
    this.shoppingDate = Objects.requireNonNull(shoppingDate, "shoppingDate must not be null");
    this.scheduleId = TextNormalizer.trimToNull(scheduleId);
    this.isAutoCreated = isAutoCreated;
    this.isArchived = isArchived;
    this.deletedAt = validateTimestamp(deletedAt, "deletedAt");
    this.createdAt = requireTimestamp(createdAt, "createdAt");
    this.updatedAt = requireTimestamp(updatedAt, "updatedAt");
  }

  private static String requireId(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static String validateName(String value) {
    String name = TextNormalizer.collapseWhitespace(value);
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("list name must not be blank");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("list name must be 80 characters or fewer");
    }
    return name;
  }

  private static Long validateTimestamp(Long value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
    return value;
  }

  private static long requireTimestamp(long value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
    return value;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public LocalDate getShoppingDate() {
    return shoppingDate;
  }

  public String getScheduleId() {
    return scheduleId;
  }

  public boolean isAutoCreated() {
    return isAutoCreated;
  }

  public boolean isArchived() {
    return isArchived;
  }

  public Long getDeletedAt() {
    return deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }
}
