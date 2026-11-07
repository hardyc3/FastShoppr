/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository.mappers;

import com.fastshoppr.app.data.local.entity.AisleCacheEntity;
import com.fastshoppr.app.domain.model.AisleCache;

public final class AisleCacheMapper {

  private AisleCacheMapper() {}

  public static AisleCacheEntity toEntity(AisleCache cache) {
    return new AisleCacheEntity(
        cache.getId(),
        cache.getStoreId(),
        cache.getNormalizedQuery(),
        cache.getAisle(),
        cache.getBay(),
        cache.getMatchedProductName(),
        cache.getMatchedProductUrl(),
        cache.getConfidence(),
        cache.getSource(),
        cache.getFetchedAt(),
        cache.getExpiresAt());
  }

  public static AisleCache toDomain(AisleCacheEntity entity) {
    return new AisleCache(
        entity.id,
        entity.storeId,
        entity.normalizedQuery,
        entity.aisle,
        entity.bay,
        entity.matchedProductName,
        entity.matchedProductUrl,
        entity.confidence,
        entity.source,
        entity.fetchedAt,
        entity.expiresAt);
  }
}
