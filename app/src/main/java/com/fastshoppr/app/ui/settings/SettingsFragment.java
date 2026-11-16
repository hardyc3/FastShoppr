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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentSettingsBinding;
import dagger.hilt.android.AndroidEntryPoint;

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
    binding.btnOpenSchedule.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.action_settings_to_schedule));

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.load();
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
