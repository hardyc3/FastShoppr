/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.StoreDao;
import com.fastshoppr.app.data.local.entity.StoreEntity;
import com.fastshoppr.app.data.repository.mappers.StoreMapper;
import com.fastshoppr.app.domain.error.NotFoundException;
import com.fastshoppr.app.domain.error.StoreInUseException;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class StoreRepositoryImpl implements StoreRepository {

  private final FastShopprDatabase database;
  private final StoreDao storeDao;
  private final Clock clock;

  @Inject
  public StoreRepositoryImpl(FastShopprDatabase database, StoreDao storeDao, Clock clock) {
    this.database = database;
    this.storeDao = storeDao;
    this.clock = clock;
  }

  @Override
  public LiveData<List<Store>> observeActive() {
    return Transformations.map(
        storeDao.observeActive(),
        entities -> entities.stream().map(StoreMapper::toDomain).toList());
  }

  @Override
  public List<Store> getActive() {
    return storeDao.getActive().stream().map(StoreMapper::toDomain).toList();
  }

  @Override
  public Store getById(String id) {
    StoreEntity entity = storeDao.getById(id);
    if (entity == null) {
      throw new NotFoundException("Store not found: " + id);
    }
    return StoreMapper.toDomain(entity);
  }

  @Override
  public Store findByBrand(StoreBrand brand) {
    StoreEntity entity = storeDao.findActiveByBrand(brand);
    return entity == null ? null : StoreMapper.toDomain(entity);
  }

  @Override
  public Store getDefault() {
    StoreEntity entity = storeDao.getDefault();
    return entity == null ? null : StoreMapper.toDomain(entity);
  }

  @Override
  public void save(Store store) {
    storeDao.upsert(StoreMapper.toEntity(store));
  }

  @Override
  public void setDefault(String id) {
    database.runInTransaction(
        () -> {
          long now = clock.nowMillis();
          storeDao.clearDefaultExcept(id, now);
          if (storeDao.setDefault(id, now) == 0) {
            throw new NotFoundException("Active store not found: " + id);
          }
        });
  }

  @Override
  public void archive(String id) {
    if (storeDao.archive(id, clock.nowMillis()) == 0) {
      throw new NotFoundException("Store not found: " + id);
    }
  }

  @Override
  public void restore(String id) {
    if (storeDao.restore(id, clock.nowMillis()) == 0) {
      throw new NotFoundException("Store not found: " + id);
    }
  }

  @Override
  public void delete(String id) {
    database.runInTransaction(
        () -> {
          if (storeDao.getById(id) == null) {
            throw new NotFoundException("Store not found: " + id);
          }
          if (storeDao.countItemReferences(id) > 0 || storeDao.countCacheReferences(id) > 0) {
            throw new StoreInUseException("Store is referenced by items or cache entries: " + id);
          }
          storeDao.deleteById(id);
        });
  }
}
