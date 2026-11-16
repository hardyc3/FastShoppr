/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentScheduleBinding;
import com.fastshoppr.app.domain.model.IntervalUnit;
import com.fastshoppr.app.domain.usecase.CreateScheduleUseCase;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@AndroidEntryPoint
public final class ScheduleFragment extends Fragment {

  private FragmentScheduleBinding binding;
  private ScheduleViewModel viewModel;
  private LocalDate nextDate;
  private ActivityResultLauncher<String> notificationPermissionLauncher;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentScheduleBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    notificationPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), granted -> { /* create silently if denied */ });
    viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);
    binding.toolbar.setTitle(R.string.schedule_title);
    binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    binding.btnPickNextDate.setOnClickListener(v -> DatePickerUtil.showDatePicker(this,
        nextDate == null ? LocalDate.now() : nextDate, date -> { nextDate = date; renderDate(); }));
    binding.btnPickPreferredTime.setOnClickListener(v -> showTimePicker());
    binding.btnClearPreferredTime.setOnClickListener(v -> binding.inputPreferredTime.setText(""));
    binding.btnSave.setOnClickListener(v -> save(CreateScheduleUseCase.ConflictAction.ASK));
    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::onEvent);
    viewModel.load();
  }

  private void render(ScheduleUiState s) {
    if (binding == null || s == null) return;
    binding.switchEnabled.setChecked(s.enabled);
    binding.inputIntervalValue.setText(String.valueOf(s.intervalValue));
    if (s.intervalUnit == IntervalUnit.WEEKS) binding.radioWeeks.setChecked(true); else binding.radioDays.setChecked(true);
    nextDate = s.nextShoppingDate;
    renderDate();
    binding.switchNotify.setChecked(s.notifyOnCreate);
    binding.inputPreferredTime.setText(s.preferredShoppingTime == null ? "" : s.preferredShoppingTime);
    binding.textStatus.setText(s.message == null ? "" : s.message);
  }

  private void renderDate() {
    if (binding != null && nextDate != null) {
      binding.textNextDate.setText(getString(R.string.field_target_date) + ": " + nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
  }

  private void onEvent(ScheduleViewModel.SaveEvent e) {
    if (e == null || binding == null) return;
    if (e.needsConflictDecision) {
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.schedule_conflict_title)
          .setMessage(R.string.schedule_conflict_message)
          .setPositiveButton(R.string.schedule_conflict_replace, (d, w) -> save(CreateScheduleUseCase.ConflictAction.DISABLE_EXISTING))
          .setNegativeButton(R.string.schedule_conflict_move, (d, w) -> save(CreateScheduleUseCase.ConflictAction.MOVE_EXISTING))
          .show();
      return;
    }
    if (e.success) {
      Toast.makeText(requireContext(),
          "disabled".equals(e.message) ? R.string.schedule_disabled_message : R.string.schedule_saved,
          Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(requireContext(), R.string.schedule_save_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void save(CreateScheduleUseCase.ConflictAction action) {
    int value = 1;
    try { value = Integer.parseInt(String.valueOf(binding.inputIntervalValue.getText()).trim()); } catch (Exception ignored) {}
    IntervalUnit unit = binding.radioWeeks.isChecked() ? IntervalUnit.WEEKS : IntervalUnit.DAYS;
    LocalDate date = nextDate == null ? LocalDate.now().plusWeeks(1) : nextDate;
    boolean enabled = binding.switchEnabled.isChecked();
    boolean notify = binding.switchNotify.isChecked();
    if (enabled && notify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
      }
    }
    viewModel.save(enabled, value, unit, date, notify,
        String.valueOf(binding.inputPreferredTime.getText()), action);
  }

  private void showTimePicker() {
    MaterialTimePicker picker = new MaterialTimePicker.Builder()
        .setTimeFormat(TimeFormat.CLOCK_12H)
        .setHour(10).setMinute(0)
        .setTitleText(R.string.schedule_preferred_time_pick)
        .build();
    picker.addOnPositiveButtonClickListener(v -> {
      int h = picker.getHour();
      int m = picker.getMinute();
      String ampm = h >= 12 ? "PM" : "AM";
      int h12 = h % 12; if (h12 == 0) h12 = 12;
      binding.inputPreferredTime.setText(String.format(Locale.US, "%d:%02d %s", h12, m, ampm));
    });
    picker.show(getParentFragmentManager(), "preferred_time");
  }

  @Override
  public void onDestroyView() { super.onDestroyView(); binding = null; }
}
