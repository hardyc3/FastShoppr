/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.Map;

/** Persists the user's chosen physical store for each supported brand. */
public interface SelectedStoreRepository {

  NearbyStore getSelected(StoreBrand brand);

  Map<StoreBrand, NearbyStore> getSelected();

  void select(NearbyStore store);

  void clear(StoreBrand brand);
}
