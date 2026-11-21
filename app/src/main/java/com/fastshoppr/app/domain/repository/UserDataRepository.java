/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import java.util.concurrent.CompletableFuture;

/** Clears every local trace of user data while keeping the built-in store catalog. */
public interface UserDataRepository {

  CompletableFuture<Void> deleteAllData();
}
