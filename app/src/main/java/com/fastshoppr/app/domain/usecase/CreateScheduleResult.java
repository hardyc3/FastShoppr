/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ShoppingSchedule;

public sealed interface CreateScheduleResult
    permits CreateScheduleResult.Created,
        CreateScheduleResult.Existing,
        CreateScheduleResult.DecisionRequired,
        CreateScheduleResult.Moved {

  record Created(ShoppingSchedule schedule) implements CreateScheduleResult {}

  record Existing(ShoppingSchedule schedule) implements CreateScheduleResult {}

  record DecisionRequired(ShoppingSchedule existing) implements CreateScheduleResult {}

  record Moved(ShoppingSchedule schedule) implements CreateScheduleResult {}
}
