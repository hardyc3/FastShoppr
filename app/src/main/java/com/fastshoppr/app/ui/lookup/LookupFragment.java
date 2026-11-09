/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import androidx.annotation.StringRes;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentPlaceholderBinding;
import com.fastshoppr.app.ui.components.PlaceholderFragment;
import com.fastshoppr.app.ui.navigation.NavArguments;

public final class LookupFragment extends PlaceholderFragment {

  @StringRes
  @Override
  protected int getTitleRes() {
    return R.string.lookup_title;
  }

  @StringRes
  @Override
  protected int getBodyRes() {
    return R.string.lookup_body;
  }

  @Override
  protected void configure(FragmentPlaceholderBinding binding) {
    setPrimaryAction(
        R.string.action_back_to_list,
        v ->
            Navigation.findNavController(v)
                .navigate(
                    R.id.list_detail, NavArguments.forList(NavArguments.PLACEHOLDER_LIST_ID)));
  }
}
