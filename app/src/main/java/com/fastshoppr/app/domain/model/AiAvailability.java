/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** Availability of the optional on-device AI model. */
public sealed interface AiAvailability
    permits AiAvailability.Available,
        AiAvailability.Downloadable,
        AiAvailability.Downloading,
        AiAvailability.Unavailable,
        AiAvailability.QuotaExceeded {

  record Available() implements AiAvailability {}

  record Downloadable() implements AiAvailability {}

  record Downloading() implements AiAvailability {}

  record Unavailable() implements AiAvailability {}

  record QuotaExceeded() implements AiAvailability {}
}
