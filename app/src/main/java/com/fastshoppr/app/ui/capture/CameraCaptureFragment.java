/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import com.fastshoppr.app.R;
import com.fastshoppr.app.ui.components.PlaceholderFragment;

public final class CameraCaptureFragment extends PlaceholderFragment {

  @Override
  protected int getTitleRes() {
    return R.string.capture_camera_toolbar_title;
  }

  @Override
  protected int getBodyRes() {
    return R.string.capture_camera_body;
  }

  @Override
  protected void configure(com.fastshoppr.app.databinding.FragmentPlaceholderBinding binding) {
    setPrimaryAction(R.string.action_continue_to_review, v -> requireActivity().finish());
  }
}
