/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.date.NearestListResolver;
import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.Test;

public final class ResolveDatePhraseUseCaseTest {

  @Test
  public void resolvesNextShoppingDayFromActiveSchedule() throws Exception {
    ScheduleRepository scheduleRepo = mock(ScheduleRepository.class);
    ShoppingListRepository listRepo = mock(ShoppingListRepository.class);
    when(listRepo.findNearestActiveOnOrAfter(LocalDate.of(2026, 11, 14))).thenReturn(null);
    ShoppingSchedule schedule = new ShoppingSchedule(
        "s1", 1, IntervalUnit.WEEKS, LocalDate.of(2026, 11, 7), true, true, null, 1, 1);
    when(scheduleRepo.getActive()).thenReturn(schedule);

    ResolveDatePhraseUseCase useCase = new ResolveDatePhraseUseCase(
        scheduleRepo, new NearestListResolver(listRepo), new DirectExecutorService());

    Optional<ResolveDatePhraseUseCase.Result> opt =
        useCase.resolve("coffee next shopping day", LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 2)).get();

    assertTrue(opt.isPresent());
    assertEquals(LocalDate.of(2026, 11, 14), opt.get().resolvedDate().orElseThrow());
    assertFalse(opt.get().requiresSchedule());
  }

  @Test
  public void nextShoppingDayWithoutScheduleHasNoResolvedDate() throws Exception {
    ScheduleRepository scheduleRepo = mock(ScheduleRepository.class);
    ShoppingListRepository listRepo = mock(ShoppingListRepository.class);
    when(scheduleRepo.getActive()).thenReturn(null);

    ResolveDatePhraseUseCase useCase = new ResolveDatePhraseUseCase(
        scheduleRepo, new NearestListResolver(listRepo), new DirectExecutorService());

    Optional<ResolveDatePhraseUseCase.Result> opt =
        useCase.resolve("coffee next shopping day", LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 2)).get();

    assertTrue(opt.isPresent());
    assertFalse(opt.get().resolvedDate().isPresent());
    assertTrue(opt.get().requiresSchedule());
  }
}
