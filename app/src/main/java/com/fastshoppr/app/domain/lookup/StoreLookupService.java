/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.lookup;

import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.concurrent.CompletableFuture;

public interface StoreLookupService {
  StoreBrand getBrand();

  CompletableFuture<StoreLookupResult> lookup(StoreLookupRequest request);
}
