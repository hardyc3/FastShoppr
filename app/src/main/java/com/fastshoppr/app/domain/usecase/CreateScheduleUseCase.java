/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class CreateScheduleUseCase {

  public enum ConflictAction {
    ASK,
    DISABLE_EXISTING,
    MOVE_EXISTING
  }

  private final ScheduleRepository scheduleRepository;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public CreateScheduleUseCase(
      ScheduleRepository scheduleRepository,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor) {
    this.scheduleRepository = scheduleRepository;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<CreateScheduleResult> create(
      int intervalValue,
      IntervalUnit intervalUnit,
      LocalDate nextShoppingDate,
      boolean notifyOnCreate) {
    return create(
        intervalValue, intervalUnit, nextShoppingDate, notifyOnCreate, ConflictAction.ASK);
  }

  public CompletableFuture<CreateScheduleResult> create(
      int intervalValue,
      IntervalUnit intervalUnit,
      LocalDate nextShoppingDate,
      boolean notifyOnCreate,
      ConflictAction conflictAction) {
    validate(intervalValue, intervalUnit, nextShoppingDate, conflictAction);
    return CompletableFuture.supplyAsync(
        () -> {
          ShoppingSchedule existing = scheduleRepository.getActive();
          if (existing == null) {
            ShoppingSchedule created =
                createSchedule(intervalValue, intervalUnit, nextShoppingDate, notifyOnCreate);
            scheduleRepository.insert(created);
            return new CreateScheduleResult.Created(created);
          }

          if (existing.getNextShoppingDate().equals(nextShoppingDate)) {
            return new CreateScheduleResult.Existing(existing);
          }

          return switch (conflictAction) {
            case ASK -> new CreateScheduleResult.DecisionRequired(existing);
            case DISABLE_EXISTING -> {
              ShoppingSchedule created =
                  createSchedule(intervalValue, intervalUnit, nextShoppingDate, notifyOnCreate);
              scheduleRepository.insert(created);
              scheduleRepository.disableOthers(created.getId());
              yield new CreateScheduleResult.Created(created);
            }
            case MOVE_EXISTING -> {
              ShoppingSchedule moved =
                  new ShoppingSchedule(
                      existing.getId(),
                      intervalValue,
                      intervalUnit,
                      nextShoppingDate,
                      true,
                      notifyOnCreate,
                      existing.getLastCreatedDate(),
                      existing.getCreatedAt(),
                      clock.nowMillis());
              scheduleRepository.update(moved);
              scheduleRepository.disableOthers(moved.getId());
              yield new CreateScheduleResult.Moved(moved);
            }
          };
        },
        executor);
  }

  private ShoppingSchedule createSchedule(
      int intervalValue,
      IntervalUnit intervalUnit,
      LocalDate nextShoppingDate,
      boolean notifyOnCreate) {
    long now = clock.nowMillis();
    return new ShoppingSchedule(
        idGenerator.newId(),
        intervalValue,
        intervalUnit,
        nextShoppingDate,
        true,
        notifyOnCreate,
        null,
        now,
        now);
  }

  private static void validate(
      int intervalValue,
      IntervalUnit intervalUnit,
      LocalDate nextShoppingDate,
      ConflictAction conflictAction) {
    if (intervalValue < 1) {
      throw new IllegalArgumentException("intervalValue must be at least 1");
    }
    Objects.requireNonNull(intervalUnit, "intervalUnit must not be null");
    Objects.requireNonNull(nextShoppingDate, "nextShoppingDate must not be null");
    Objects.requireNonNull(conflictAction, "conflictAction must not be null");
  }
}
