/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.observation.Observable;
import java.util.List;

public interface StoreRepository {

  Observable<List<Store>> observeActive();

  List<Store> getActive();

  Store getById(String id);

  Store findByBrand(StoreBrand brand);

  Store getDefault();

  void save(Store store);

  void setDefault(String id);

  void archive(String id);

  void restore(String id);

  void delete(String id);
}
