/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import java.util.Map;
import javax.inject.Inject;

/** Reads the stores the user has saved for aisle verification. */
public final class GetSelectedStoresUseCase {

  private final SelectedStoreRepository repository;

  @Inject
  public GetSelectedStoresUseCase(SelectedStoreRepository repository) {
    this.repository = repository;
  }

  public NearbyStore execute(StoreBrand brand) {
    return repository.getSelected(brand);
  }

  public Map<StoreBrand, NearbyStore> execute() {
    return repository.getSelected();
  }

  public String getPostalCode() {
    return repository.getPostalCode();
  }
}
