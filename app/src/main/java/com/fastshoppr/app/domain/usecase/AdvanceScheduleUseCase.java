/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class AdvanceScheduleUseCase {

  private final ScheduleRepository scheduleRepository;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public AdvanceScheduleUseCase(
      ScheduleRepository scheduleRepository, Clock clock, ExecutorService executor) {
    this.scheduleRepository = scheduleRepository;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<ShoppingSchedule> advance(String scheduleId) {
    return CompletableFuture.supplyAsync(
        () -> {
          ShoppingSchedule schedule = scheduleRepository.getById(scheduleId);
          LocalDate nextDate =
              switch (schedule.getIntervalUnit()) {
                case DAYS -> schedule.getNextShoppingDate().plusDays(schedule.getIntervalValue());
                case WEEKS -> schedule.getNextShoppingDate().plusWeeks(schedule.getIntervalValue());
              };
          ShoppingSchedule advanced =
              new ShoppingSchedule(
                  schedule.getId(),
                  schedule.getIntervalValue(),
                  schedule.getIntervalUnit(),
                  nextDate,
                  schedule.isEnabled(),
                  schedule.isNotifyOnCreate(),
                  schedule.getNextShoppingDate(),
                  schedule.getCreatedAt(),
                  clock.nowMillis());
          scheduleRepository.update(advanced);
          return advanced;
        },
        executor);
  }
}
