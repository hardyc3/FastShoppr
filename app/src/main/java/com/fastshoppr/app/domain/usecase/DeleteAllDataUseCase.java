/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.repository.UserDataRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** User-initiated reset: removes lists, captures, cache, settings, and queued work. */
public final class DeleteAllDataUseCase {

  private final UserDataRepository repository;

  @Inject
  public DeleteAllDataUseCase(UserDataRepository repository) {
    this.repository = repository;
  }

  public CompletableFuture<Void> execute() {
    return repository.deleteAllData();
  }
}
