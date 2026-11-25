/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.PermissionAction;
import javax.inject.Inject;

/**
 * Pure permission decision. A system prompt is never issued without a prior, acknowledged
 * rationale.
 *
 * @param granted whether Android has already granted the permission
 * @param rationale whether the in-app rationale was shown and acknowledged for this attempt
 * @param permanentlyDenied whether the user denied a previous request and Android will not prompt
 *     again
 */
public final class RequestPermissionUseCase {

  @Inject
  public RequestPermissionUseCase() {}

  public PermissionAction resolve(boolean granted, boolean rationale, boolean permanentlyDenied) {
    if (granted) {
      return PermissionAction.NONE;
    }
    if (permanentlyDenied) {
      return PermissionAction.SETTINGS;
    }
    return rationale ? PermissionAction.REQUEST : PermissionAction.RATIONALE;
  }
}
