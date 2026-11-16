/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.CreateScheduleResult;
import com.fastshoppr.app.domain.usecase.CreateScheduleUseCase;
import com.fastshoppr.app.work.ScheduleWorkerScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class ScheduleViewModel extends ViewModel {

  public static final class SaveEvent {
    public final boolean success;
    public final String message;
    public final boolean needsConflictDecision;
    public final ShoppingSchedule existing;

    public SaveEvent(boolean success, String message, boolean needsConflictDecision, ShoppingSchedule existing) {
      this.success = success;
      this.message = message;
      this.needsConflictDecision = needsConflictDecision;
      this.existing = existing;
    }
  }

  private final ScheduleRepository scheduleRepository;
  private final SettingsRepository settingsRepository;
  private final CreateScheduleUseCase createScheduleUseCase;
  private final ScheduleWorkerScheduler scheduleWorkerScheduler;
  private final Clock clock;
  private final ExecutorService executor;

  private final MutableLiveData<ScheduleUiState> state = new MutableLiveData<>();
  private final MutableLiveData<SaveEvent> events = new MutableLiveData<>();

  @Inject
  public ScheduleViewModel(
      ScheduleRepository scheduleRepository,
      SettingsRepository settingsRepository,
      CreateScheduleUseCase createScheduleUseCase,
      ScheduleWorkerScheduler scheduleWorkerScheduler,
      Clock clock,
      ExecutorService executor) {
    this.scheduleRepository = scheduleRepository;
    this.settingsRepository = settingsRepository;
    this.createScheduleUseCase = createScheduleUseCase;
    this.scheduleWorkerScheduler = scheduleWorkerScheduler;
    this.clock = clock;
    this.executor = executor;
  }

  public LiveData<ScheduleUiState> getState() { return state; }
  public LiveData<SaveEvent> getEvents() { return events; }

  public void load() {
    executor.execute(() -> {
      ShoppingSchedule active = scheduleRepository.getActive();
      LocalDate today = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
      if (active == null) {
        state.postValue(new ScheduleUiState(false, 1, IntervalUnit.WEEKS, today.plusWeeks(1), false,
            settingsRepository.getPreferredShoppingTime(), null));
      } else {
        state.postValue(new ScheduleUiState(active.isEnabled(), active.getIntervalValue(),
            active.getIntervalUnit(), active.getNextShoppingDate(), active.isNotifyOnCreate(),
            settingsRepository.getPreferredShoppingTime(), null));
      }
    });
  }

  public void save(boolean enabled, int intervalValue, IntervalUnit unit, LocalDate nextDate,
      boolean notifyOnCreate, String preferredTime, CreateScheduleUseCase.ConflictAction conflictAction) {
    executor.execute(() -> {
      try {
        settingsRepository.setPreferredShoppingTime(
            preferredTime == null || preferredTime.trim().isEmpty() ? null : preferredTime.trim());
        ShoppingSchedule active = scheduleRepository.getActive();
        if (!enabled) {
          if (active != null) {
            ShoppingSchedule disabled = new ShoppingSchedule(active.getId(), active.getIntervalValue(),
                active.getIntervalUnit(), active.getNextShoppingDate(), false, active.isNotifyOnCreate(),
                active.getLastCreatedDate(), active.getCreatedAt(), clock.nowMillis());
            scheduleRepository.update(disabled);
          }
          events.postValue(new SaveEvent(true, "disabled", false, null));
          load();
          return;
        }
        CreateScheduleResult result = createScheduleUseCase.create(intervalValue, unit, nextDate, notifyOnCreate, conflictAction).join();
        if (result instanceof CreateScheduleResult.DecisionRequired dr) {
          events.postValue(new SaveEvent(false, "conflict", true, dr.getExisting()));
          return;
        }
        scheduleWorkerScheduler.enqueueImmediateCheck();
        events.postValue(new SaveEvent(true, "saved", false, null));
        load();
      } catch (Exception e) {
        events.postValue(new SaveEvent(false, e.getMessage() == null ? "failed" : e.getMessage(), false, null));
      }
    });
  }
}
