/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import com.fastshoppr.app.data.local.dao.AisleCacheDao;
import com.fastshoppr.app.data.local.entity.AisleCacheEntity;
import com.fastshoppr.app.data.repository.mappers.AisleCacheMapper;
import com.fastshoppr.app.domain.model.AisleCache;
import com.fastshoppr.app.domain.repository.AisleCacheRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class AisleCacheRepositoryImpl implements AisleCacheRepository {

  private final AisleCacheDao cacheDao;

  @Inject
  public AisleCacheRepositoryImpl(AisleCacheDao cacheDao) {
    this.cacheDao = cacheDao;
  }

  @Override
  public AisleCache find(String storeId, String normalizedQuery) {
    AisleCacheEntity entity = cacheDao.find(storeId, normalizedQuery);
    return entity == null ? null : AisleCacheMapper.toDomain(entity);
  }

  @Override
  public void put(AisleCache cache) {
    cacheDao.upsert(AisleCacheMapper.toEntity(cache));
  }

  @Override
  public void delete(String id) {
    cacheDao.deleteById(id);
  }

  @Override
  public void deleteByStore(String storeId) {
    cacheDao.deleteByStore(storeId);
  }

  @Override
  public int deleteExpired(long nowMillis) {
    return cacheDao.deleteExpired(nowMillis);
  }
}
