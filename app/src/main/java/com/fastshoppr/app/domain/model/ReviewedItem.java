/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/** User-approved text from the review screen, ready to become a {@link ListItem}. */
public record ReviewedItem(String text, String storeId, LocalDate targetDate) {

  public ReviewedItem {
    Objects.requireNonNull(text, "text must not be null");
    Objects.requireNonNull(storeId, "storeId must not be null");
    Objects.requireNonNull(targetDate, "targetDate must not be null");
  }
}
