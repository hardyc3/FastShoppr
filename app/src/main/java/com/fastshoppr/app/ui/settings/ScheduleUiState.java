/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import com.fastshoppr.app.domain.model.IntervalUnit;
import java.time.LocalDate;

public final class ScheduleUiState {
  public final boolean enabled;
  public final int intervalValue;
  public final IntervalUnit intervalUnit;
  public final LocalDate nextShoppingDate;
  public final boolean notifyOnCreate;
  public final String preferredShoppingTime;
  public final String message;

  public ScheduleUiState(
      boolean enabled,
      int intervalValue,
      IntervalUnit intervalUnit,
      LocalDate nextShoppingDate,
      boolean notifyOnCreate,
      String preferredShoppingTime,
      String message) {
    this.enabled = enabled;
    this.intervalValue = intervalValue;
    this.intervalUnit = intervalUnit;
    this.nextShoppingDate = nextShoppingDate;
    this.notifyOnCreate = notifyOnCreate;
    this.preferredShoppingTime = preferredShoppingTime;
    this.message = message;
  }
}
