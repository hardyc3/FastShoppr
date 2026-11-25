/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.domain.model.PermissionAction;
import org.junit.Test;

public final class RequestPermissionUseCaseTest {

  private final RequestPermissionUseCase resolver = new RequestPermissionUseCase();

  @Test
  public void grantedNeedsNoAction() {
    assertEquals(PermissionAction.NONE, resolver.resolve(true, false, false));
    assertEquals(PermissionAction.NONE, resolver.resolve(true, true, true));
  }

  @Test
  public void permanentDenialRoutesToSettings() {
    assertEquals(PermissionAction.SETTINGS, resolver.resolve(false, false, true));
    assertEquals(PermissionAction.SETTINGS, resolver.resolve(false, true, true));
  }

  @Test
  public void firstRequestShowsRationaleBeforeSystemPrompt() {
    assertEquals(PermissionAction.RATIONALE, resolver.resolve(false, false, false));
  }

  @Test
  public void acknowledgedRationaleAllowsSystemRequest() {
    assertEquals(PermissionAction.REQUEST, resolver.resolve(false, true, false));
  }
}
