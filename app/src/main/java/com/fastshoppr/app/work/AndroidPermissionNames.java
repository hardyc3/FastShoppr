/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.Manifest;
import com.fastshoppr.app.domain.model.PlatformPermission;

/** Maps domain permission names to Android platform constants. */
final class AndroidPermissionNames {

  private AndroidPermissionNames() {}

  static String platformName(PlatformPermission permission) {
    return switch (permission) {
      case CAMERA -> Manifest.permission.CAMERA;
      case POST_NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS;
    };
  }
}
