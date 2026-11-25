/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.date.LocalDateTimes;
import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.PermissionAction;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.notification.NotificationPermissionStatus;
import com.fastshoppr.app.domain.port.PermissionRequester;
import com.fastshoppr.app.domain.port.PermissionStatus;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.CreateScheduleResult;
import com.fastshoppr.app.domain.usecase.CreateScheduleUseCase;
import com.fastshoppr.app.domain.usecase.RequestPermissionUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.work.ScheduleWorkerScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class ScheduleViewModel extends ViewModel {

  private static final PlatformPermission PERMISSION = PlatformPermission.POST_NOTIFICATIONS;
  private static final String KEY_PERMISSION_PREVIOUSLY_REQUESTED = "schedulePermissionRequested";
  private static final String KEY_RATIONALE_ACKNOWLEDGED = "scheduleRationaleAcknowledged";

  public static final class SaveEvent {
    public final boolean success;
    public final String message;
    public final boolean needsConflictDecision;
    public final ShoppingSchedule existing;

    public SaveEvent(
        boolean success, String message, boolean needsConflictDecision, ShoppingSchedule existing) {
      this.success = success;
      this.message = message;
      this.needsConflictDecision = needsConflictDecision;
      this.existing = existing;
    }
  }

  private final ScheduleRepository scheduleRepository;
  private final CreateScheduleUseCase createScheduleUseCase;
  private final ScheduleWorkerScheduler scheduleWorkerScheduler;
  private final Clock clock;
  private final ExecutorService executor;
  private final NotificationPermissionStatus notificationPermissionStatus;
  private final PermissionStatus permissionStatus;
  private final PermissionRequester permissionRequester;
  private final RequestPermissionUseCase permissionResolver;
  private final SavedStateHandle savedStateHandle;

  private record PendingSave(
      boolean enabled,
      int intervalValue,
      IntervalUnit unit,
      LocalDate date,
      boolean notifyOnCreate,
      CreateScheduleUseCase.ConflictAction conflictAction) {}

  private final MutableLiveData<ScheduleUiState> state = new MutableLiveData<>();
  private final MutableLiveData<SaveEvent> events = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<PermissionAction>> permissionActions =
      new MutableLiveData<>();

  private boolean permissionPreviouslyRequested;
  private boolean rationaleAcknowledged;
  private boolean lastPermissionGranted;
  private boolean lastShouldShowRationale;
  private PendingSave pendingSave;

  @Inject
  public ScheduleViewModel(
      ScheduleRepository scheduleRepository,
      CreateScheduleUseCase createScheduleUseCase,
      ScheduleWorkerScheduler scheduleWorkerScheduler,
      Clock clock,
      ExecutorService executor,
      NotificationPermissionStatus notificationPermissionStatus,
      PermissionStatus permissionStatus,
      PermissionRequester permissionRequester,
      RequestPermissionUseCase permissionResolver,
      SavedStateHandle savedStateHandle) {
    this.scheduleRepository = scheduleRepository;
    this.createScheduleUseCase = createScheduleUseCase;
    this.scheduleWorkerScheduler = scheduleWorkerScheduler;
    this.clock = clock;
    this.executor = executor;
    this.notificationPermissionStatus = notificationPermissionStatus;
    this.permissionStatus = permissionStatus;
    this.permissionRequester = permissionRequester;
    this.permissionResolver = permissionResolver;
    this.savedStateHandle = savedStateHandle;
    permissionPreviouslyRequested =
        Boolean.TRUE.equals(savedStateHandle.get(KEY_PERMISSION_PREVIOUSLY_REQUESTED));
    rationaleAcknowledged = Boolean.TRUE.equals(savedStateHandle.get(KEY_RATIONALE_ACKNOWLEDGED));
  }

  public LiveData<ScheduleUiState> getState() {
    return state;
  }

  public LiveData<SaveEvent> getEvents() {
    return events;
  }

  public LiveData<SingleEvent<PermissionAction>> getPermissionActions() {
    return permissionActions;
  }

  /** Presentational dates come from the injected clock so they are deterministic and testable. */
  public LocalDate today() {
    return LocalDateTimes.today(clock);
  }

  public LocalDate defaultNextDate() {
    return today().plusWeeks(1);
  }

  /** Reads permission state through the domain port and performs the decided action. */
  public void checkPermission() {
    checkPermission(
        permissionStatus.isGranted(PERMISSION), permissionStatus.shouldShowRationale(PERMISSION));
  }

  private void checkPermission(boolean granted, boolean shouldShowRationale) {
    lastPermissionGranted = granted;
    lastShouldShowRationale = shouldShowRationale;
    boolean permanentlyDenied = !granted && permissionPreviouslyRequested && !shouldShowRationale;
    boolean rationaleDone = rationaleAcknowledged;
    PermissionAction action = permissionResolver.resolve(granted, rationaleDone, permanentlyDenied);
    if (action == PermissionAction.REQUEST) {
      permissionRequester.request(PERMISSION, this::permissionResult);
      return;
    }
    if (action != PermissionAction.RATIONALE) {
      commitPendingSave();
    }
    permissionActions.setValue(new SingleEvent<>(action));
  }

  /** Continues the save without a prompt when the user declines the rationale. */
  public void permissionRationaleDeclined() {
    commitPendingSave();
  }

  public void rationaleAccepted() {
    rationaleAcknowledged = true;
    savedStateHandle.set(KEY_RATIONALE_ACKNOWLEDGED, true);
    checkPermission(lastPermissionGranted, lastShouldShowRationale);
  }

  public void permissionResult(boolean granted) {
    permissionPreviouslyRequested = true;
    savedStateHandle.set(KEY_PERMISSION_PREVIOUSLY_REQUESTED, true);
    rationaleAcknowledged = false;
    savedStateHandle.set(KEY_RATIONALE_ACKNOWLEDGED, false);
    checkPermission(granted, permissionStatus.shouldShowRationale(PERMISSION));
  }

  public void load() {
    executor.execute(
        () -> {
          ShoppingSchedule active = scheduleRepository.getActive();
          if (active == null) {
            state.postValue(
                new ScheduleUiState(false, 1, IntervalUnit.WEEKS, defaultNextDate(), false, null));
          } else {
            state.postValue(
                new ScheduleUiState(
                    active.isEnabled(),
                    active.getIntervalValue(),
                    active.getIntervalUnit(),
                    active.getNextShoppingDate(),
                    active.isNotifyOnCreate(),
                    null));
          }
        });
  }

  /** Stores the form decision in the ViewModel and asks for permission only when required. */
  public void requestSave(
      boolean enabled,
      int intervalValue,
      IntervalUnit unit,
      LocalDate nextDate,
      boolean notifyOnCreate,
      CreateScheduleUseCase.ConflictAction conflictAction) {
    PendingSave request =
        new PendingSave(enabled, intervalValue, unit, nextDate, notifyOnCreate, conflictAction);
    if (!needsNotificationPermission(request)) {
      commit(request);
      return;
    }
    pendingSave = request;
    checkPermission();
  }

  /** Continues a save that was paused for a permission decision. */
  public void commitPendingSave() {
    if (pendingSave == null) {
      return;
    }
    PendingSave request = pendingSave;
    pendingSave = null;
    commit(request);
  }

  private boolean needsNotificationPermission(PendingSave request) {
    return notificationPermissionStatus.isRuntimePermissionRequired()
        && request.enabled()
        && request.notifyOnCreate()
        && !permissionStatus.isGranted(PERMISSION);
  }

  private void commit(PendingSave request) {
    executor.execute(
        () -> {
          try {
            ShoppingSchedule active = scheduleRepository.getActive();
            if (!request.enabled()) {
              if (active != null) {
                ShoppingSchedule disabled =
                    new ShoppingSchedule(
                        active.getId(),
                        active.getIntervalValue(),
                        active.getIntervalUnit(),
                        active.getNextShoppingDate(),
                        false,
                        active.isNotifyOnCreate(),
                        active.getLastCreatedDate(),
                        active.getCreatedAt(),
                        clock.nowMillis());
                scheduleRepository.update(disabled);
              }
              events.postValue(new SaveEvent(true, "disabled", false, null));
              load();
              return;
            }
            CreateScheduleResult result =
                createScheduleUseCase
                    .create(
                        request.intervalValue(),
                        request.unit(),
                        request.date(),
                        request.notifyOnCreate(),
                        request.conflictAction())
                    .join();
            if (result instanceof CreateScheduleResult.DecisionRequired dr) {
              events.postValue(new SaveEvent(false, "conflict", true, dr.existing()));
              return;
            }
            scheduleWorkerScheduler.enqueueImmediateCheck();
            events.postValue(new SaveEvent(true, "saved", false, null));
            load();
          } catch (Exception e) {
            events.postValue(
                new SaveEvent(
                    false, e.getMessage() == null ? "failed" : e.getMessage(), false, null));
          }
        });
  }
}
