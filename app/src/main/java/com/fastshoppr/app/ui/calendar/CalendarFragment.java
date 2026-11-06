/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import androidx.annotation.StringRes;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentPlaceholderBinding;
import com.fastshoppr.app.ui.components.PlaceholderFragment;

public final class CalendarFragment extends PlaceholderFragment {

  @StringRes
  @Override
  protected int getTitleRes() {
    return R.string.calendar_title;
  }

  @StringRes
  @Override
  protected int getBodyRes() {
    return R.string.calendar_body;
  }

  @Override
  protected void configure(FragmentPlaceholderBinding binding) {
    setPrimaryAction(
        R.string.action_open_lists, v -> Navigation.findNavController(v).navigate(R.id.lists));
  }
}
