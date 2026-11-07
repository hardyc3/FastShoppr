/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

public final class CreateScheduleUseCaseTest {

  private static final LocalDate DATE = LocalDate.of(2026, 11, 7);
  private static final long NOW = 1_000L;

  private ScheduleRepository repository;
  private CreateScheduleUseCase useCase;

  @Before
  public void setUp() {
    repository = mock(ScheduleRepository.class);
    useCase =
        new CreateScheduleUseCase(
            repository, new FixedIdGenerator(), new FixedClock(NOW), new DirectExecutorService());
  }

  @Test
  public void createsScheduleWhenNoneExists() throws Exception {
    when(repository.getActive()).thenReturn(null);

    CreateScheduleResult result = useCase.create(2, IntervalUnit.WEEKS, DATE, true).get();

    assertTrue(result instanceof CreateScheduleResult.Created);
    ShoppingSchedule created = ((CreateScheduleResult.Created) result).schedule();
    assertEquals("id-1", created.getId());
    assertEquals(DATE, created.getNextShoppingDate());
    assertEquals(IntervalUnit.WEEKS, created.getIntervalUnit());
    assertTrue(created.isEnabled());
    verify(repository).insert(created);
    verify(repository, never()).disableOthers(anyString());
  }

  @Test
  public void returnsExistingScheduleForSameDate() throws Exception {
    ShoppingSchedule existing = schedule("existing", DATE, IntervalUnit.WEEKS, 2, true);
    when(repository.getActive()).thenReturn(existing);

    CreateScheduleResult result = useCase.create(2, IntervalUnit.WEEKS, DATE, false).get();

    assertTrue(result instanceof CreateScheduleResult.Existing);
    assertEquals(existing, ((CreateScheduleResult.Existing) result).schedule());
    verify(repository, never()).insert(any());
    verify(repository, never()).update(any());
  }

  @Test
  public void asksForDecisionWhenActiveScheduleHasDifferentDate() throws Exception {
    ShoppingSchedule existing =
        schedule("existing", DATE.plusWeeks(1), IntervalUnit.WEEKS, 2, true);
    when(repository.getActive()).thenReturn(existing);

    CreateScheduleResult result = useCase.create(2, IntervalUnit.WEEKS, DATE, true).get();

    assertTrue(result instanceof CreateScheduleResult.DecisionRequired);
    assertEquals(existing, ((CreateScheduleResult.DecisionRequired) result).existing());
    verify(repository, never()).insert(any());
    verify(repository, never()).update(any());
  }

  @Test
  public void disablesExistingScheduleWhenChosen() throws Exception {
    ShoppingSchedule existing =
        schedule("existing", DATE.plusWeeks(1), IntervalUnit.WEEKS, 2, true);
    when(repository.getActive()).thenReturn(existing);

    CreateScheduleResult result =
        useCase
            .create(
                1,
                IntervalUnit.DAYS,
                DATE,
                false,
                CreateScheduleUseCase.ConflictAction.DISABLE_EXISTING)
            .get();

    assertTrue(result instanceof CreateScheduleResult.Created);
    ShoppingSchedule created = ((CreateScheduleResult.Created) result).schedule();
    assertEquals("id-1", created.getId());
    verify(repository).insert(created);
    verify(repository).disableOthers(created.getId());
  }

  @Test
  public void movesExistingScheduleWhenChosen() throws Exception {
    ShoppingSchedule existing =
        schedule("existing", DATE.plusWeeks(1), IntervalUnit.WEEKS, 2, true);
    when(repository.getActive()).thenReturn(existing);

    CreateScheduleResult result =
        useCase
            .create(
                3,
                IntervalUnit.DAYS,
                DATE,
                false,
                CreateScheduleUseCase.ConflictAction.MOVE_EXISTING)
            .get();

    assertTrue(result instanceof CreateScheduleResult.Moved);
    ShoppingSchedule moved = ((CreateScheduleResult.Moved) result).schedule();
    assertEquals("existing", moved.getId());
    assertEquals(DATE, moved.getNextShoppingDate());
    assertEquals(3, moved.getIntervalValue());
    assertEquals(IntervalUnit.DAYS, moved.getIntervalUnit());
    assertFalse(moved.isNotifyOnCreate());
    assertEquals(existing.getCreatedAt(), moved.getCreatedAt());
    assertEquals(NOW, moved.getUpdatedAt());
    verify(repository).update(moved);
    verify(repository).disableOthers("existing");
  }

  private static ShoppingSchedule schedule(
      String id, LocalDate date, IntervalUnit unit, int intervalValue, boolean enabled) {
    return new ShoppingSchedule(id, intervalValue, unit, date, enabled, true, null, 1, 1);
  }
}
