/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.fragment.app.Fragment;

/** Opens this app's system settings screen for permanent permission denial. */
public final class AppSettingsOpener {

  private AppSettingsOpener() {}

  public static void open(Fragment fragment) {
    Intent intent =
        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", fragment.requireContext().getPackageName(), null));
    fragment.startActivity(intent);
  }
}
