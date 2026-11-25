/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentScheduleBinding;
import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.model.PermissionAction;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.usecase.CreateScheduleUseCase;
import com.fastshoppr.app.ui.common.AppSettingsOpener;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.PermissionLauncherBinder;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;

@AndroidEntryPoint
public final class ScheduleFragment extends Fragment {

  private FragmentScheduleBinding binding;
  private ScheduleViewModel viewModel;
  private LocalDate nextDate;

  @Inject PermissionLauncherBinder permissionRequester;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    permissionRequester.bind(PlatformPermission.POST_NOTIFICATIONS, this);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentScheduleBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);
    binding.toolbar.setTitle(R.string.schedule_title);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());
    binding.btnPickNextDate.setOnClickListener(
        v ->
            DatePickerUtil.showDatePicker(
                this,
                nextDate == null ? viewModel.today() : nextDate,
                date -> {
                  nextDate = date;
                  renderDate();
                }));
    binding.btnSave.setOnClickListener(v -> save(CreateScheduleUseCase.ConflictAction.ASK));
    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::onEvent);
    viewModel.getPermissionActions().observe(getViewLifecycleOwner(), this::handlePermissionAction);
    viewModel.load();
  }

  private void render(ScheduleUiState s) {
    if (binding == null || s == null) return;
    binding.switchEnabled.setChecked(s.enabled);
    binding.inputIntervalValue.setText(String.valueOf(s.intervalValue));
    if (s.intervalUnit == IntervalUnit.WEEKS) binding.radioWeeks.setChecked(true);
    else binding.radioDays.setChecked(true);
    nextDate = s.nextShoppingDate;
    renderDate();
    binding.switchNotify.setChecked(s.notifyOnCreate);
    binding.textStatus.setText(s.message == null ? "" : s.message);
  }

  private void renderDate() {
    if (binding != null && nextDate != null) {
      binding.textNextDate.setText(
          getString(
              R.string.schedule_next_date_value,
              nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }
  }

  private void onEvent(ScheduleViewModel.SaveEvent e) {
    if (e == null || binding == null) return;
    if (e.needsConflictDecision) {
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.schedule_conflict_title)
          .setMessage(R.string.schedule_conflict_message)
          .setPositiveButton(
              R.string.schedule_conflict_replace,
              (d, w) -> save(CreateScheduleUseCase.ConflictAction.DISABLE_EXISTING))
          .setNegativeButton(
              R.string.schedule_conflict_move,
              (d, w) -> save(CreateScheduleUseCase.ConflictAction.MOVE_EXISTING))
          .show();
      return;
    }
    if (e.success) {
      Toast.makeText(
              requireContext(),
              "disabled".equals(e.message)
                  ? R.string.schedule_disabled_message
                  : R.string.schedule_saved,
              Toast.LENGTH_SHORT)
          .show();
    } else {
      Toast.makeText(requireContext(), R.string.schedule_save_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void handlePermissionAction(SingleEvent<PermissionAction> event) {
    PermissionAction action = event == null ? null : event.getContentIfNotHandled();
    if (action == null || binding == null) {
      return;
    }
    switch (action) {
      case NONE -> {
        // The ViewModel already committed the pending save for this action.
      }
      case RATIONALE ->
          new MaterialAlertDialogBuilder(requireContext())
              .setMessage(R.string.schedule_permission_rationale)
              .setNegativeButton(
                  android.R.string.cancel, (d, w) -> viewModel.permissionRationaleDeclined())
              .setPositiveButton(R.string.ok, (d, w) -> viewModel.rationaleAccepted())
              .show();
      case REQUEST -> {
        // The ViewModel consumes REQUEST by calling PermissionRequester directly.
      }
      case SETTINGS -> AppSettingsOpener.open(this);
    }
  }

  private void save(CreateScheduleUseCase.ConflictAction action) {
    int value = 1;
    try {
      value = Integer.parseInt(String.valueOf(binding.inputIntervalValue.getText()).trim());
    } catch (Exception ignored) {
    }
    IntervalUnit unit = binding.radioWeeks.isChecked() ? IntervalUnit.WEEKS : IntervalUnit.DAYS;
    LocalDate date = nextDate == null ? viewModel.defaultNextDate() : nextDate;
    viewModel.requestSave(
        binding.switchEnabled.isChecked(),
        value,
        unit,
        date,
        binding.switchNotify.isChecked(),
        action);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onDestroy() {
    permissionRequester.unbind(PlatformPermission.POST_NOTIFICATIONS, this);
    super.onDestroy();
  }
}
