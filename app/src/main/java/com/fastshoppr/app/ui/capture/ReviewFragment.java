/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import androidx.annotation.StringRes;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentPlaceholderBinding;
import com.fastshoppr.app.ui.components.PlaceholderFragment;
import com.fastshoppr.app.ui.navigation.NavArguments;

public final class ReviewFragment extends PlaceholderFragment {

  @StringRes
  @Override
  protected int getTitleRes() {
    return R.string.review_title;
  }

  @StringRes
  @Override
  protected int getBodyRes() {
    return R.string.review_body;
  }

  @Override
  protected void configure(FragmentPlaceholderBinding binding) {
    setPrimaryAction(
        R.string.action_continue_to_lookup,
        v ->
            Navigation.findNavController(v)
                .navigate(R.id.lookup, NavArguments.forList(NavArguments.PLACEHOLDER_LIST_ID)));
  }
}
