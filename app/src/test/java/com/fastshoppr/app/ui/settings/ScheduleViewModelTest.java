/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.domain.model.PermissionAction;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.notification.NotificationPermissionStatus;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.usecase.CreateScheduleUseCase;
import com.fastshoppr.app.domain.usecase.RequestPermissionUseCase;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FakePermissionPorts;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.work.ScheduleWorkerScheduler;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public final class ScheduleViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  private static final long NOW = Instant.parse("2026-11-24T12:00:00Z").toEpochMilli();

  private final List<PermissionAction> permissionActions = new ArrayList<>();
  private final FakePermissionPorts.Status permissionStatus =
      new FakePermissionPorts.Status(false, false);
  private final FakePermissionPorts.Requester permissionRequester =
      new FakePermissionPorts.Requester();
  private ScheduleViewModel viewModel;

  @Before
  public void setUp() {
    permissionActions.clear();
    viewModel =
        new ScheduleViewModel(
            mock(ScheduleRepository.class),
            mock(CreateScheduleUseCase.class),
            mock(ScheduleWorkerScheduler.class),
            new FixedClock(NOW),
            new DirectExecutorService(),
            mock(NotificationPermissionStatus.class),
            permissionStatus,
            permissionRequester,
            new RequestPermissionUseCase(),
            new SavedStateHandle(Map.of()));
    viewModel
        .getPermissionActions()
        .observeForever(
            event -> {
              PermissionAction action = actionOf(event);
              if (action != null) {
                permissionActions.add(action);
              }
            });
  }

  @Test
  public void dateDefaultsComeFromInjectedClock() {
    LocalDate today = Instant.ofEpochMilli(NOW).atZone(ZoneId.systemDefault()).toLocalDate();

    assertEquals(today, viewModel.today());
    assertEquals(today.plusWeeks(1), viewModel.defaultNextDate());
  }

  @Test
  public void firstPermissionCheckAsksForRationaleBeforeSystemRequest() {
    viewModel.checkPermission();

    assertEquals(List.of(PermissionAction.RATIONALE), permissionActions);
  }

  @Test
  public void acceptedRationaleRequestsPermissionAndResultRoutesPermanentDenialToSettings() {
    viewModel.checkPermission();
    viewModel.rationaleAccepted();
    assertEquals(List.of(PlatformPermission.POST_NOTIFICATIONS), permissionRequester.requests);

    permissionRequester.result(false);
    assertEquals(List.of(PermissionAction.RATIONALE, PermissionAction.SETTINGS), permissionActions);
  }

  @Test
  public void rationaleAcceptedRequestsPermissionEvenWhenRationaleFlagRemainsTrue() {
    permissionStatus.set(false, true);
    viewModel.checkPermission();
    viewModel.rationaleAccepted();

    assertEquals(List.of(PermissionAction.RATIONALE), permissionActions);
    assertEquals(List.of(PlatformPermission.POST_NOTIFICATIONS), permissionRequester.requests);
  }

  @Test
  public void saveWaitsForNotificationPermissionDecisionInViewModel() {
    ScheduleRepository repository = mock(ScheduleRepository.class);
    NotificationPermissionStatus status = mock(NotificationPermissionStatus.class);
    FakePermissionPorts.Status targetStatus = new FakePermissionPorts.Status(false, false);
    FakePermissionPorts.Requester targetRequester = new FakePermissionPorts.Requester();
    when(status.isRuntimePermissionRequired()).thenReturn(true);
    ScheduleViewModel target =
        new ScheduleViewModel(
            repository,
            mock(CreateScheduleUseCase.class),
            mock(ScheduleWorkerScheduler.class),
            new FixedClock(NOW),
            new DirectExecutorService(),
            status,
            targetStatus,
            targetRequester,
            new RequestPermissionUseCase(),
            new SavedStateHandle(Map.of()));
    List<PermissionAction> actions = new ArrayList<>();
    target
        .getPermissionActions()
        .observeForever(
            event -> {
              PermissionAction action = actionOf(event);
              if (action != null) {
                actions.add(action);
              }
            });

    target.requestSave(
        true,
        1,
        com.fastshoppr.app.domain.model.IntervalUnit.WEEKS,
        LocalDate.of(2026, 12, 1),
        true,
        CreateScheduleUseCase.ConflictAction.ASK);

    assertEquals(List.of(PermissionAction.RATIONALE), actions);
    verify(repository, never()).getActive();
    target.rationaleAccepted();
    assertEquals(List.of(PlatformPermission.POST_NOTIFICATIONS), targetRequester.requests);
    targetRequester.result(true);
    verify(repository, atLeastOnce()).getActive();
  }

  @Test
  public void grantedPermissionNeedsNoAction() {
    permissionStatus.set(true, false);
    viewModel.checkPermission();

    assertEquals(List.of(PermissionAction.NONE), permissionActions);
  }

  @Test
  public void permanentDenialSurvivesSavedStateRestore() {
    SavedStateHandle handle = new SavedStateHandle(Map.of());
    FakePermissionPorts.Status status = new FakePermissionPorts.Status(false, false);
    FakePermissionPorts.Requester requester = new FakePermissionPorts.Requester();
    ScheduleViewModel first =
        new ScheduleViewModel(
            mock(ScheduleRepository.class),
            mock(CreateScheduleUseCase.class),
            mock(ScheduleWorkerScheduler.class),
            new FixedClock(NOW),
            new DirectExecutorService(),
            mock(NotificationPermissionStatus.class),
            status,
            requester,
            new RequestPermissionUseCase(),
            handle);
    first.permissionResult(false);

    ScheduleViewModel restored =
        new ScheduleViewModel(
            mock(ScheduleRepository.class),
            mock(CreateScheduleUseCase.class),
            mock(ScheduleWorkerScheduler.class),
            new FixedClock(NOW),
            new DirectExecutorService(),
            mock(NotificationPermissionStatus.class),
            status,
            requester,
            new RequestPermissionUseCase(),
            handle);
    List<PermissionAction> restoredActions = new ArrayList<>();
    restored
        .getPermissionActions()
        .observeForever(
            event -> {
              PermissionAction action = actionOf(event);
              if (action != null) {
                restoredActions.add(action);
              }
            });

    restored.checkPermission();

    assertEquals(List.of(PermissionAction.SETTINGS), restoredActions);
  }

  private static PermissionAction actionOf(SingleEvent<PermissionAction> event) {
    return event == null ? null : event.getContentIfNotHandled();
  }
}
