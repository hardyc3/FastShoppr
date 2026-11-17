/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import java.time.LocalDate;
import org.junit.Test;

public final class AdvanceScheduleUseCaseTest {

  @Test
  public void advancesByDays() throws Exception {
    ScheduleRepository repository = mock(ScheduleRepository.class);
    ShoppingSchedule schedule =
        new ShoppingSchedule(
            "id", 3, IntervalUnit.DAYS, LocalDate.of(2026, 11, 7), true, true, null, 1, 1);
    when(repository.getById("id")).thenReturn(schedule);

    ShoppingSchedule result =
        new AdvanceScheduleUseCase(repository, new FixedClock(10), new DirectExecutorService())
            .advance("id")
            .get();

    assertEquals(LocalDate.of(2026, 11, 10), result.getNextShoppingDate());
    assertEquals(LocalDate.of(2026, 11, 7), result.getLastCreatedDate());
    assertEquals(10, result.getUpdatedAt());
    verify(repository).update(result);
  }

  @Test
  public void advancesByWeeks() throws Exception {
    ScheduleRepository repository = mock(ScheduleRepository.class);
    ShoppingSchedule schedule =
        new ShoppingSchedule(
            "id", 2, IntervalUnit.WEEKS, LocalDate.of(2026, 11, 7), true, true, null, 1, 1);
    when(repository.getById("id")).thenReturn(schedule);

    ShoppingSchedule result =
        new AdvanceScheduleUseCase(repository, new FixedClock(10), new DirectExecutorService())
            .advance("id")
            .get();

    assertEquals(LocalDate.of(2026, 11, 21), result.getNextShoppingDate());
  }

  @Test
  public void advancesAcrossMonthEndUsingCalendarMath() throws Exception {
    ScheduleRepository repository = mock(ScheduleRepository.class);
    ShoppingSchedule schedule =
        new ShoppingSchedule(
            "id", 1, IntervalUnit.WEEKS, LocalDate.of(2026, 1, 29), true, true, null, 1, 1);
    when(repository.getById("id")).thenReturn(schedule);

    ShoppingSchedule result =
        new AdvanceScheduleUseCase(repository, new FixedClock(10), new DirectExecutorService())
            .advance("id")
            .get();

    assertEquals(LocalDate.of(2026, 2, 5), result.getNextShoppingDate());
  }

  @Test
  public void advancesAcrossSpringForwardWithoutShiftingDate() throws Exception {
    ScheduleRepository repository = mock(ScheduleRepository.class);
    ShoppingSchedule schedule =
        new ShoppingSchedule(
            "id", 1, IntervalUnit.WEEKS, LocalDate.of(2026, 3, 7), true, true, null, 1, 1);
    when(repository.getById("id")).thenReturn(schedule);

    ShoppingSchedule result =
        new AdvanceScheduleUseCase(repository, new FixedClock(10), new DirectExecutorService())
            .advance("id")
            .get();

    assertEquals(LocalDate.of(2026, 3, 14), result.getNextShoppingDate());
  }

  @Test
  public void advancesAcrossFallBackWithoutShiftingDate() throws Exception {
    ScheduleRepository repository = mock(ScheduleRepository.class);
    ShoppingSchedule schedule =
        new ShoppingSchedule(
            "id", 1, IntervalUnit.WEEKS, LocalDate.of(2026, 10, 31), true, true, null, 1, 1);
    when(repository.getById("id")).thenReturn(schedule);

    ShoppingSchedule result =
        new AdvanceScheduleUseCase(repository, new FixedClock(10), new DirectExecutorService())
            .advance("id")
            .get();

    assertEquals(LocalDate.of(2026, 11, 7), result.getNextShoppingDate());
  }
}
