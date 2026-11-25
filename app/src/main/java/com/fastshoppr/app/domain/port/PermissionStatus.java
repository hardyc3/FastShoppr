/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.port;

import com.fastshoppr.app.domain.model.PlatformPermission;

/** Reads platform permission state without exposing Android types to callers. */
public interface PermissionStatus {

  boolean isGranted(PlatformPermission permission);

  boolean shouldShowRationale(PlatformPermission permission);
}
