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
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentSettingsBinding;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.ui.common.StoreBrandLabel;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AndroidEntryPoint
public final class SettingsFragment extends Fragment {

  private FragmentSettingsBinding binding;
  private SettingsViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentSettingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

    binding.toolbar.setTitle(R.string.settings_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    binding.switchAiCleanup.setOnCheckedChangeListener(
        (buttonView, isChecked) -> viewModel.setAiCleanupEnabled(isChecked));
    binding.btnDownloadModel.setOnClickListener(v -> viewModel.downloadModel());
    binding.btnDeleteInkModel.setOnClickListener(v -> viewModel.deleteInkModel());
    binding.btnOpenSchedule.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.action_settings_to_schedule));
    binding.btnDeleteAllData.setOnClickListener(v -> confirmDeleteAllData());

    binding.btnPickBranch.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(
                    R.id.action_settings_to_branch_picker, NavArguments.forBranch(currentBrand())));
    binding.btnForgetSavedStore.setOnClickListener(this::showForgetStoreDialog);

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel
        .getStoreMessageEvent()
        .observe(
            getViewLifecycleOwner(),
            event -> {
              SettingsViewModel.StoreMessage message = event.getContentIfNotHandled();
              if (message == null || binding == null) {
                return;
              }
              int messageRes =
                  message == SettingsViewModel.StoreMessage.STORE_CLEAR_FAILED
                      ? R.string.settings_store_clear_failed_message
                      : R.string.settings_store_removed_message;
              Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_LONG).show();
            });
    viewModel
        .getInkModelDeleteEvent()
        .observe(
            getViewLifecycleOwner(),
            event -> {
              Boolean success = event.getContentIfNotHandled();
              if (success == null || binding == null) {
                return;
              }
              Snackbar.make(
                      binding.getRoot(),
                      success
                          ? R.string.settings_ink_model_delete_success
                          : R.string.settings_ink_model_delete_failed,
                      Snackbar.LENGTH_LONG)
                  .show();
            });
    viewModel
        .getDeleteEvent()
        .observe(
            getViewLifecycleOwner(),
            event -> {
              Boolean success = event.getContentIfNotHandled();
              if (success == null || binding == null) {
                return;
              }
              if (success) {
                Navigation.findNavController(binding.getRoot()).popBackStack();
              } else {
                Snackbar.make(
                        binding.getRoot(),
                        R.string.settings_delete_all_failed,
                        Snackbar.LENGTH_LONG)
                    .show();
              }
            });
    viewModel.load();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (viewModel != null) {
      viewModel.refresh();
    }
  }

  private StoreBrand currentBrand() {
    StoreBrand[] brands = StoreBrandLabel.locatorBrands();
    int position = binding.storeBrandSpinner.getSelectedItemPosition();
    return position >= 0 && position < brands.length ? brands[position] : StoreBrand.WALMART;
  }

  private void showForgetStoreDialog(View view) {
    SettingsViewModel.SettingsUiState state = viewModel.getState().getValue();
    if (state == null || state.getSelectedStores().isEmpty()) {
      Snackbar.make(view, R.string.settings_store_no_saved, Snackbar.LENGTH_LONG).show();
      return;
    }
    Map<StoreBrand, NearbyStore> selected = state.getSelectedStores();
    List<StoreBrand> brands = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    for (StoreBrand brand : StoreBrandLabel.locatorBrands()) {
      NearbyStore store = selected.get(brand);
      if (store != null) {
        brands.add(brand);
        labels.add(getString(StoreBrandLabel.labelRes(brand)) + ": " + store.getLabel());
      }
    }
    View dialogView =
        LayoutInflater.from(requireContext()).inflate(R.layout.dialog_forget_store, null);
    RadioGroup options = dialogView.findViewById(R.id.forgetStoreOptions);
    Map<Integer, StoreBrand> brandByOption = new HashMap<>();
    for (int index = 0; index < brands.size(); index++) {
      MaterialRadioButton option = new MaterialRadioButton(requireContext());
      option.setId(View.generateViewId());
      option.setText(labels.get(index));
      options.addView(option);
      brandByOption.put(option.getId(), brands.get(index));
    }
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.settings_store_forget_title)
        .setView(dialogView)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.settings_store_remove,
            (dialog, which) -> {
              StoreBrand brand = brandByOption.get(options.getCheckedRadioButtonId());
              if (brand != null) {
                viewModel.forgetStore(brand);
              }
            })
        .show();
  }

  private void confirmDeleteAllData() {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.settings_delete_all_title)
        .setMessage(R.string.settings_delete_all_message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.settings_delete_all_confirm, (dialog, which) -> viewModel.deleteAllData())
        .show();
  }

  private void render(SettingsViewModel.SettingsUiState state) {
    if (binding == null) {
      return;
    }
    if (binding.switchAiCleanup.isChecked() != state.isAiCleanupEnabled()) {
      binding.switchAiCleanup.setOnCheckedChangeListener(null);
      binding.switchAiCleanup.setChecked(state.isAiCleanupEnabled());
      binding.switchAiCleanup.setOnCheckedChangeListener(
          (buttonView, isChecked) -> viewModel.setAiCleanupEnabled(isChecked));
    }
    binding.aiStatus.setText(statusTextRes(state));
    if (state.getAiStatus() == SettingsViewModel.AiStatus.DOWNLOADING) {
      binding.aiStatus.setText(
          getString(
              R.string.settings_ai_downloading_bytes,
              state.getDownloadedBytes() / (1024L * 1024L)));
    }
    binding.btnDownloadModel.setVisibility(
        state.getAiStatus() == SettingsViewModel.AiStatus.DOWNLOADABLE ? View.VISIBLE : View.GONE);
    binding.inkModelStatus.setText(inkModelStatusTextRes(state));
    binding.btnDeleteInkModel.setEnabled(
        state.getInkModelDeleteStatus() != SettingsViewModel.InkModelDeleteStatus.DELETING);
    binding.storeSummary.setText(formatStoreSummary(state));
  }

  private String formatStoreSummary(SettingsViewModel.SettingsUiState state) {
    if (state.getSelectedStores().isEmpty()) {
      return getString(R.string.settings_store_no_saved);
    }
    List<String> lines = new ArrayList<>();
    for (StoreBrand brand : StoreBrandLabel.locatorBrands()) {
      NearbyStore store = state.getSelectedStores().get(brand);
      if (store != null) {
        lines.add(
            getString(
                R.string.settings_store_saved_format,
                getString(StoreBrandLabel.labelRes(brand)),
                store.getLabel()));
      }
    }
    return String.join("\n", lines);
  }

  private int inkModelStatusTextRes(SettingsViewModel.SettingsUiState state) {
    return switch (state.getInkModelDeleteStatus()) {
      case IDLE -> R.string.settings_ink_model_idle;
      case DELETING -> R.string.settings_ink_model_deleting;
      case DELETED -> R.string.settings_ink_model_deleted;
      case FAILED -> R.string.settings_ink_model_failed;
    };
  }

  private int statusTextRes(SettingsViewModel.SettingsUiState state) {
    return switch (state.getAiStatus()) {
      case CHECKING -> R.string.settings_ai_checking;
      case AVAILABLE -> R.string.settings_ai_available;
      case DOWNLOADABLE -> R.string.settings_ai_downloadable;
      case DOWNLOADING -> R.string.settings_ai_downloading;
      case QUOTA_EXCEEDED -> R.string.settings_ai_quota;
      case DOWNLOAD_FAILED -> R.string.settings_ai_download_failed;
      case UNAVAILABLE -> R.string.settings_ai_unavailable;
    };
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
