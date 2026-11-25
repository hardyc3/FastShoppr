/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.notification.ListCreationNotifier;
import com.fastshoppr.app.domain.notification.NotificationPermissionStatus;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;

public final class CreateScheduledListUseCaseTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

  private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
  private final ShoppingListRepository listRepository = mock(ShoppingListRepository.class);
  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final SettingsRepository settingsRepository = mock(SettingsRepository.class);
  private final AdvanceScheduleUseCase advanceScheduleUseCase = mock(AdvanceScheduleUseCase.class);
  private final FixedClock clock =
      new FixedClock(Instant.parse("2026-11-24T12:00:00Z").toEpochMilli());
  private final List<ShoppingList> notifications = new ArrayList<>();

  private CreateScheduledListUseCase useCase;

  @Before
  public void setUp() {
    NotificationPermissionStatus permission = () -> true;
    ListCreationNotifier notifier =
        list -> {
          notifications.add(list);
          return true;
        };
    useCase =
        new CreateScheduledListUseCase(
            scheduleRepository,
            listRepository,
            storeRepository,
            settingsRepository,
            advanceScheduleUseCase,
            clock,
            new DirectExecutorService(),
            permission,
            notifier);
    when(settingsRepository.areNotificationsEnabled()).thenReturn(true);
    when(listRepository.getOrCreate(any(), anyString(), anyString(), anyBoolean()))
        .thenAnswer(
            invocation -> {
              LocalDate date = invocation.getArgument(0);
              return newShoppingList(date);
            });
    when(advanceScheduleUseCase.advance(anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));
  }

  private static ShoppingList newShoppingList(LocalDate date) {
    return new ShoppingList(
        "list-" + date, "Shopping list " + date, date, "schedule-1", true, false, null, 1L, 1L);
  }

  private static ShoppingSchedule schedule(LocalDate nextDate, boolean notify) {
    return new ShoppingSchedule(
        "schedule-1", 1, IntervalUnit.WEEKS, nextDate, true, notify, null, 1L, 1L);
  }

  @Test
  public void createsAndNotifiesForDueSchedule() {
    when(scheduleRepository.getActive()).thenReturn(schedule(TODAY.minusDays(1), true));
    when(scheduleRepository.getById("schedule-1"))
        .thenReturn(schedule(TODAY.minusDays(1), true), schedule(TODAY.plusDays(6), true));

    CreateScheduledListUseCase.Result result = useCase.runIfDue().join();

    assertTrue(result.created());
    assertEquals(TODAY.minusDays(1), result.list().getShoppingDate());
    assertTrue(result.notified());
    assertEquals(1, notifications.size());
    assertSame(result.list(), notifications.get(0));
    verify(advanceScheduleUseCase, times(1)).advance("schedule-1");
  }

  @Test
  public void doesNothingWhenScheduleIsNotDue() {
    when(scheduleRepository.getActive()).thenReturn(schedule(TODAY.plusDays(1), true));

    CreateScheduledListUseCase.Result result = useCase.runIfDue().join();

    assertFalse(result.created());
    assertFalse(result.notified());
    verify(listRepository, never()).getOrCreate(any(), anyString(), anyString(), anyBoolean());
    assertTrue(notifications.isEmpty());
  }

  @Test
  public void skipsNotificationWhenSystemPermissionDenied() {
    when(scheduleRepository.getActive()).thenReturn(schedule(TODAY, true));
    when(scheduleRepository.getById("schedule-1")).thenReturn(schedule(TODAY.plusDays(7), true));
    useCase =
        new CreateScheduledListUseCase(
            scheduleRepository,
            listRepository,
            storeRepository,
            settingsRepository,
            advanceScheduleUseCase,
            clock,
            new DirectExecutorService(),
            () -> false,
            list -> true);

    CreateScheduledListUseCase.Result result = useCase.runIfDue().join();

    assertTrue(result.created());
    assertFalse(result.notified());
    assertTrue(notifications.isEmpty());
  }

  @Test
  public void skipsNotificationWhenAppSettingDisabled() {
    when(settingsRepository.areNotificationsEnabled()).thenReturn(false);
    when(scheduleRepository.getActive()).thenReturn(schedule(TODAY, true));
    when(scheduleRepository.getById("schedule-1")).thenReturn(schedule(TODAY.plusDays(7), true));

    CreateScheduledListUseCase.Result result = useCase.runIfDue().join();

    assertTrue(result.created());
    assertFalse(result.notified());
    assertTrue(notifications.isEmpty());
  }

  @Test
  public void doesNotCreateBacklogWhenSeveralDatesAreOverdue() {
    when(scheduleRepository.getActive()).thenReturn(schedule(TODAY.minusDays(14), true));
    when(scheduleRepository.getById("schedule-1"))
        .thenReturn(
            schedule(TODAY.minusDays(14), true),
            schedule(TODAY.minusDays(7), true),
            schedule(TODAY.plusDays(7), true));

    CreateScheduledListUseCase.Result result = useCase.runIfDue().join();

    assertTrue(result.created());
    verify(advanceScheduleUseCase, times(2)).advance("schedule-1");
    verify(listRepository, times(1))
        .getOrCreate(any(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true));
  }
}
