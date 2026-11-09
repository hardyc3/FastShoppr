/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import androidx.fragment.app.Fragment;
import com.fastshoppr.app.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Offers retry, gallery, and typed options when recognition returns nothing. */
final class EmptyRecognitionDialog {

  private EmptyRecognitionDialog() {}

  static void show(Fragment fragment, Runnable onRetry, Runnable onGallery, Runnable onType) {
    if (fragment.getActivity() == null) {
      return;
    }
    new MaterialAlertDialogBuilder(fragment.requireContext())
        .setTitle(R.string.capture_no_text_title)
        .setMessage(R.string.review_empty)
        .setNeutralButton(R.string.capture_retry, (dialog, which) -> onRetry.run())
        .setNegativeButton(R.string.capture_pick_another, (dialog, which) -> onGallery.run())
        .setPositiveButton(R.string.capture_type_items, (dialog, which) -> onType.run())
        .show();
  }
}
