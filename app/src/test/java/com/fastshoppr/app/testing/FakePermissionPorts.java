/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.port.PermissionRequester;
import com.fastshoppr.app.domain.port.PermissionStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Deterministic permission ports for ViewModel tests. */
public final class FakePermissionPorts {

  private FakePermissionPorts() {}

  public static final class Status implements PermissionStatus {
    private boolean granted;
    private boolean showRationale;

    public Status(boolean granted, boolean showRationale) {
      this.granted = granted;
      this.showRationale = showRationale;
    }

    public void set(boolean granted, boolean showRationale) {
      this.granted = granted;
      this.showRationale = showRationale;
    }

    @Override
    public boolean isGranted(PlatformPermission permission) {
      return granted;
    }

    @Override
    public boolean shouldShowRationale(PlatformPermission permission) {
      return showRationale;
    }
  }

  public static final class Requester implements PermissionRequester {
    public final List<PlatformPermission> requests = new ArrayList<>();
    private Consumer<Boolean> callback;

    @Override
    public void request(PlatformPermission permission, Consumer<Boolean> result) {
      requests.add(permission);
      callback = result;
    }

    public void result(boolean granted) {
      if (callback == null) {
        throw new IllegalStateException("No permission request recorded");
      }
      callback.accept(granted);
    }
  }
}
