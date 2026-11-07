/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.AisleCache;

public interface AisleCacheRepository {

  AisleCache find(String storeId, String normalizedQuery);

  void put(AisleCache cache);

  void delete(String id);

  void deleteByStore(String storeId);

  int deleteExpired(long nowMillis);
}
