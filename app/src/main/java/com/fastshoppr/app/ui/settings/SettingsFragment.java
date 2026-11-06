/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import androidx.annotation.StringRes;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentPlaceholderBinding;
import com.fastshoppr.app.ui.components.PlaceholderFragment;

public final class SettingsFragment extends PlaceholderFragment {

  @StringRes
  @Override
  protected int getTitleRes() {
    return R.string.settings_title;
  }

  @StringRes
  @Override
  protected int getBodyRes() {
    return R.string.settings_body;
  }

  @Override
  protected void configure(FragmentPlaceholderBinding binding) {
    setPrimaryAction(R.string.action_back, v -> Navigation.findNavController(v).popBackStack());
  }
}
