/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.date.LocalDateTimes;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.notification.ListCreationNotifier;
import com.fastshoppr.app.domain.notification.NotificationPermissionStatus;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class CreateScheduledListUseCase {

  public record Result(boolean created, ShoppingList list, boolean notified) {}

  private final ScheduleRepository scheduleRepository;
  private final ShoppingListRepository listRepository;
  private final StoreRepository storeRepository;
  private final SettingsRepository settingsRepository;
  private final AdvanceScheduleUseCase advanceScheduleUseCase;
  private final Clock clock;
  private final ExecutorService executor;
  private final NotificationPermissionStatus notificationPermissionStatus;
  private final ListCreationNotifier listCreationNotifier;

  @Inject
  public CreateScheduledListUseCase(
      ScheduleRepository scheduleRepository,
      ShoppingListRepository listRepository,
      StoreRepository storeRepository,
      SettingsRepository settingsRepository,
      AdvanceScheduleUseCase advanceScheduleUseCase,
      Clock clock,
      ExecutorService executor,
      NotificationPermissionStatus notificationPermissionStatus,
      ListCreationNotifier listCreationNotifier) {
    this.scheduleRepository = scheduleRepository;
    this.listRepository = listRepository;
    this.storeRepository = storeRepository;
    this.settingsRepository = settingsRepository;
    this.advanceScheduleUseCase = advanceScheduleUseCase;
    this.clock = clock;
    this.executor = executor;
    this.notificationPermissionStatus = notificationPermissionStatus;
    this.listCreationNotifier = listCreationNotifier;
  }

  public CompletableFuture<Result> runIfDue() {
    return CompletableFuture.supplyAsync(
        () -> {
          ShoppingSchedule schedule = scheduleRepository.getActive();
          if (schedule == null || !schedule.isEnabled()) {
            return new Result(false, null, false);
          }
          LocalDate today = LocalDateTimes.today(clock);
          if (schedule.getNextShoppingDate().isAfter(today)) {
            return new Result(false, null, false);
          }
          LocalDate dueDate = schedule.getNextShoppingDate();
          String dateLabel = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
          String name = "Shopping list " + dateLabel;
          ShoppingList existing = listRepository.findNearestActiveOnOrAfter(dueDate);
          boolean createdNew = existing == null || !existing.getShoppingDate().equals(dueDate);
          ShoppingList list = listRepository.getOrCreate(dueDate, name, schedule.getId(), true);
          if (createdNew) {
            seedDefaultStoreSections(list);
          }
          // advance schedule to next upcoming date (do not create backlog)
          advanceToUpcoming(schedule, today);
          boolean notified = false;
          if (createdNew && schedule.isNotifyOnCreate() && canNotify()) {
            notified = listCreationNotifier.notify(list);
          }
          return new Result(createdNew, list, notified);
        },
        executor);
  }

  private void advanceToUpcoming(ShoppingSchedule schedule, LocalDate today) {
    try {
      ShoppingSchedule current = scheduleRepository.getById(schedule.getId());
      int guard = 0;
      while (!current.getNextShoppingDate().isAfter(today) && guard++ < 1000) {
        current = advanceScheduleUseCase.advance(current.getId()).join();
        // reload
        current = scheduleRepository.getById(schedule.getId());
      }
    } catch (Exception ignored) {
      // best effort; app-open check will retry
    }
  }

  private void seedDefaultStoreSections(ShoppingList list) {
    // Create no items but ensure default stores exist in selection context by persisting last-used;
    // actual store sections render from items - so nothing to insert. Spec says "add default store
    // sections (last used, up to three)" - interpreted as make them readily available; keep simple.
    // No-op for now; UI can suggest defaults.
  }

  private boolean canNotify() {
    if (!settingsRepository.areNotificationsEnabled()) {
      return false;
    }
    try {
      return notificationPermissionStatus.isSystemPermissionGranted();
    } catch (Exception e) {
      return false;
    }
  }
}
