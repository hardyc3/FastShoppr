/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.GeoPoint;
import java.util.concurrent.CompletableFuture;

/** Provides the last known device location after the user grants coarse location. */
public interface LocationSource {

  /** Returns {@code null} when no recent fix exists or permission was denied. */
  CompletableFuture<GeoPoint> lastKnownLocation();
}
