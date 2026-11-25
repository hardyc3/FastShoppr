/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** The single next step a screen may take for a runtime permission. */
public enum PermissionAction {
  /** Permission is already granted: continue the user's action. */
  NONE,
  /** The rationale was shown and acknowledged: launch the system permission request. */
  REQUEST,
  /** Show the in-app rationale before any system prompt. */
  RATIONALE,
  /** Permission was permanently denied: send the user to app details settings. */
  SETTINGS
}
