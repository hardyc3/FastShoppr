/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import com.fastshoppr.app.domain.text.TextNormalizer;
import java.time.LocalDate;
import java.util.Objects;

public final class ListItem {

  private static final int MAX_TEXT_LENGTH = 200;

  private final String id;
  private final String listId;
  private final String storeId;
  private final String rawText;
  private final String displayName;
  private final String normalizedText;
  private final String quantity;
  private final LocalDate targetDate;
  private final String aisle;
  private final String bay;
  private final String matchedProductName;
  private final String matchedProductUrl;
  private final Float matchConfidence;
  private final LookupStatus lookupStatus;
  private final LookupSource lookupSource;
  private final String lookupError;
  private final ItemSource source;
  private final boolean isChecked;
  private final int sortPosition;
  private final Long deletedAt;
  private final long createdAt;
  private final long updatedAt;

  public ListItem(
      String id,
      String listId,
      String storeId,
      String rawText,
      String displayName,
      String normalizedText,
      String quantity,
      LocalDate targetDate,
      String aisle,
      String bay,
      String matchedProductName,
      String matchedProductUrl,
      Float matchConfidence,
      LookupStatus lookupStatus,
      LookupSource lookupSource,
      String lookupError,
      ItemSource source,
      boolean isChecked,
      int sortPosition,
      Long deletedAt,
      long createdAt,
      long updatedAt) {
    this.id = requireId(id, "item id");
    this.listId = requireId(listId, "list id");
    this.storeId = requireId(storeId, "store id");
    this.rawText = validateRawText(rawText);
    this.displayName = validateDisplayName(displayName, this.rawText);
    this.normalizedText = validateNormalizedText(normalizedText, this.displayName);
    this.quantity = TextNormalizer.trimToNull(quantity);
    this.targetDate = Objects.requireNonNull(targetDate, "targetDate must not be null");
    this.aisle = TextNormalizer.trimToNull(aisle);
    this.bay = TextNormalizer.trimToNull(bay);
    this.matchedProductName = TextNormalizer.trimToNull(matchedProductName);
    this.matchedProductUrl = TextNormalizer.trimToNull(matchedProductUrl);
    this.matchConfidence = validateConfidence(matchConfidence);
    this.lookupStatus = Objects.requireNonNull(lookupStatus, "lookupStatus must not be null");
    this.lookupSource = lookupSource;
    this.lookupError = TextNormalizer.trimToNull(lookupError);
    this.source = Objects.requireNonNull(source, "source must not be null");
    this.isChecked = isChecked;
    if (sortPosition < 0) {
      throw new IllegalArgumentException("sortPosition must not be negative");
    }
    this.sortPosition = sortPosition;
    this.deletedAt = validateNullableTimestamp(deletedAt, "deletedAt");
    this.createdAt = requireTimestamp(createdAt, "createdAt");
    this.updatedAt = requireTimestamp(updatedAt, "updatedAt");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builderFrom(ListItem item) {
    return new Builder().from(item);
  }

  private static String requireId(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static String validateRawText(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("rawText must not be blank");
    }
    String rawText = value.trim();
    if (rawText.length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException("rawText must be 200 characters or fewer");
    }
    return rawText;
  }

  private static String validateDisplayName(String value, String rawText) {
    String displayName = TextNormalizer.collapseWhitespace(value);
    if (displayName == null || displayName.isEmpty()) {
      displayName = rawText;
    }
    if (displayName.length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException("displayName must be 200 characters or fewer");
    }
    return displayName;
  }

  private static String validateNormalizedText(String value, String displayName) {
    if (value == null) {
      return TextNormalizer.normalizeForLookup(displayName);
    }
    return TextNormalizer.collapseWhitespace(value);
  }

  private static Float validateConfidence(Float value) {
    if (value != null && (value < 0.0f || value > 1.0f)) {
      throw new IllegalArgumentException("matchConfidence must be between 0 and 1");
    }
    return value;
  }

  private static long requireTimestamp(long value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
    return value;
  }

  private static Long validateNullableTimestamp(Long value, String field) {
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

  public String getStoreId() {
    return storeId;
  }

  public String getRawText() {
    return rawText;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getNormalizedText() {
    return normalizedText;
  }

  public String getQuantity() {
    return quantity;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public String getAisle() {
    return aisle;
  }

  public String getBay() {
    return bay;
  }

  public String getMatchedProductName() {
    return matchedProductName;
  }

  public String getMatchedProductUrl() {
    return matchedProductUrl;
  }

  public Float getMatchConfidence() {
    return matchConfidence;
  }

  public LookupStatus getLookupStatus() {
    return lookupStatus;
  }

  public LookupSource getLookupSource() {
    return lookupSource;
  }

  public String getLookupError() {
    return lookupError;
  }

  public ItemSource getSource() {
    return source;
  }

  public boolean isChecked() {
    return isChecked;
  }

  public int getSortPosition() {
    return sortPosition;
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

  public static final class Builder {

    private String id;
    private String listId;
    private String storeId;
    private String rawText;
    private String displayName;
    private String normalizedText;
    private String quantity;
    private LocalDate targetDate;
    private String aisle;
    private String bay;
    private String matchedProductName;
    private String matchedProductUrl;
    private Float matchConfidence;
    private LookupStatus lookupStatus = LookupStatus.PENDING;
    private LookupSource lookupSource;
    private String lookupError;
    private ItemSource source = ItemSource.TYPED;
    private boolean isChecked;
    private int sortPosition;
    private Long deletedAt;
    private long createdAt;
    private long updatedAt;

    private Builder() {}

    private Builder from(ListItem item) {
      id = item.id;
      listId = item.listId;
      storeId = item.storeId;
      rawText = item.rawText;
      displayName = item.displayName;
      normalizedText = item.normalizedText;
      quantity = item.quantity;
      targetDate = item.targetDate;
      aisle = item.aisle;
      bay = item.bay;
      matchedProductName = item.matchedProductName;
      matchedProductUrl = item.matchedProductUrl;
      matchConfidence = item.matchConfidence;
      lookupStatus = item.lookupStatus;
      lookupSource = item.lookupSource;
      lookupError = item.lookupError;
      source = item.source;
      isChecked = item.isChecked;
      sortPosition = item.sortPosition;
      deletedAt = item.deletedAt;
      createdAt = item.createdAt;
      updatedAt = item.updatedAt;
      return this;
    }

    public Builder id(String value) {
      id = value;
      return this;
    }

    public Builder listId(String value) {
      listId = value;
      return this;
    }

    public Builder storeId(String value) {
      storeId = value;
      return this;
    }

    public Builder rawText(String value) {
      rawText = value;
      return this;
    }

    public Builder displayName(String value) {
      displayName = value;
      return this;
    }

    public Builder normalizedText(String value) {
      normalizedText = value;
      return this;
    }

    public Builder quantity(String value) {
      quantity = value;
      return this;
    }

    public Builder targetDate(LocalDate value) {
      targetDate = value;
      return this;
    }

    public Builder aisle(String value) {
      aisle = value;
      return this;
    }

    public Builder bay(String value) {
      bay = value;
      return this;
    }

    public Builder matchedProductName(String value) {
      matchedProductName = value;
      return this;
    }

    public Builder matchedProductUrl(String value) {
      matchedProductUrl = value;
      return this;
    }

    public Builder matchConfidence(Float value) {
      matchConfidence = value;
      return this;
    }

    public Builder lookupStatus(LookupStatus value) {
      lookupStatus = value;
      return this;
    }

    public Builder lookupSource(LookupSource value) {
      lookupSource = value;
      return this;
    }

    public Builder lookupError(String value) {
      lookupError = value;
      return this;
    }

    public Builder source(ItemSource value) {
      source = value;
      return this;
    }

    public Builder isChecked(boolean value) {
      isChecked = value;
      return this;
    }

    public Builder sortPosition(int value) {
      sortPosition = value;
      return this;
    }

    public Builder deletedAt(Long value) {
      deletedAt = value;
      return this;
    }

    public Builder createdAt(long value) {
      createdAt = value;
      return this;
    }

    public Builder updatedAt(long value) {
      updatedAt = value;
      return this;
    }

    public ListItem build() {
      return new ListItem(
          id,
          listId,
          storeId,
          rawText,
          displayName,
          normalizedText,
          quantity,
          targetDate,
          aisle,
          bay,
          matchedProductName,
          matchedProductUrl,
          matchConfidence,
          lookupStatus,
          lookupSource,
          lookupError,
          source,
          isChecked,
          sortPosition,
          deletedAt,
          createdAt,
          updatedAt);
    }
  }
}
