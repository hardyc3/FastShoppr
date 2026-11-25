/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import androidx.fragment.app.Fragment;
import com.fastshoppr.app.domain.model.PlatformPermission;

/** UI-owned lifecycle hook for the Android runtime-permission launcher. */
public interface PermissionLauncherBinder {

  void bind(PlatformPermission permission, Fragment fragment);

  void unbind(PlatformPermission permission, Fragment fragment);
}
