/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.notification;

/** Reads whether the system currently allows the app to post notifications. */
public interface NotificationPermissionStatus {

  boolean isSystemPermissionGranted();

  /** Whether this device requires a runtime notification permission request. */
  default boolean isRuntimePermissionRequired() {
    return false;
  }
}
