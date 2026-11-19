/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import javax.inject.Inject;

/** Removes a saved branch so the user can choose a different one. */
public final class ForgetSelectedStoreUseCase {

  private final SelectedStoreRepository repository;

  @Inject
  public ForgetSelectedStoreUseCase(SelectedStoreRepository repository) {
    this.repository = repository;
  }

  public void execute(StoreBrand brand) {
    repository.clear(brand);
  }
}
