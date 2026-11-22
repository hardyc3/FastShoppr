/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import javax.inject.Inject;

/** Saves the branch the user confirmed for a brand. */
public final class SaveSelectedStoreUseCase {

  private final SelectedStoreRepository repository;

  @Inject
  public SaveSelectedStoreUseCase(SelectedStoreRepository repository) {
    this.repository = repository;
  }

  public void execute(NearbyStore store) {
    repository.select(store);
  }
}
