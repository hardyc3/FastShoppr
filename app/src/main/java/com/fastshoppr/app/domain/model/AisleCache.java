/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import com.fastshoppr.app.domain.text.TextNormalizer;
import java.util.Objects;

public final class AisleCache {

  private final String id;
  private final String storeId;
  private final String normalizedQuery;
  private final String aisle;
  private final String bay;
  private final String matchedProductName;
  private final String matchedProductUrl;
  private final Float confidence;
  private final LookupSource source;
  private final long fetchedAt;
  private final long expiresAt;

  public AisleCache(
      String id,
      String storeId,
      String normalizedQuery,
      String aisle,
      String bay,
      String matchedProductName,
      String matchedProductUrl,
      Float confidence,
      LookupSource source,
      long fetchedAt,
      long expiresAt) {
    this.id = requireId(id, "cache id");
    this.storeId = requireId(storeId, "store id");
    this.normalizedQuery = requireId(normalizedQuery, "normalized query");
    this.aisle = TextNormalizer.trimToNull(aisle);
    this.bay = TextNormalizer.trimToNull(bay);
    this.matchedProductName = TextNormalizer.trimToNull(matchedProductName);
    this.matchedProductUrl = TextNormalizer.trimToNull(matchedProductUrl);
    this.confidence = validateConfidence(confidence);
    this.source = Objects.requireNonNull(source, "source must not be null");
    this.fetchedAt = requireTimestamp(fetchedAt, "fetchedAt");
    this.expiresAt = requireTimestamp(expiresAt, "expiresAt");
  }

  private static String requireId(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static Float validateConfidence(Float value) {
    if (value != null && (value < 0.0f || value > 1.0f)) {
      throw new IllegalArgumentException("confidence must be between 0 and 1");
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

  public String getStoreId() {
    return storeId;
  }

  public String getNormalizedQuery() {
    return normalizedQuery;
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

  public Float getConfidence() {
    return confidence;
  }

  public LookupSource getSource() {
    return source;
  }

  public long getFetchedAt() {
    return fetchedAt;
  }

  public long getExpiresAt() {
    return expiresAt;
  }

  public boolean isExpired(long nowMillis) {
    return nowMillis > expiresAt;
  }
}
