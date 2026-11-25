/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.port.PermissionRequester;
import com.fastshoppr.app.ui.common.PermissionLauncherBinder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Binds one launcher per Fragment, while ViewModels issue the actual request. */
@Singleton
public final class AndroidPermissionRequester
    implements PermissionRequester, PermissionLauncherBinder {

  private final Map<PlatformPermission, List<Binding>> bindings =
      new EnumMap<>(PlatformPermission.class);

  @Inject
  public AndroidPermissionRequester() {}

  @Override
  public void bind(PlatformPermission permission, Fragment fragment) {
    ActivityResultLauncher<String> launcher =
        fragment.registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> consumeResult(permission, fragment, granted));
    synchronized (this) {
      removeBinding(permission, fragment);
      bindings
          .computeIfAbsent(permission, ignored -> new ArrayList<>())
          .add(new Binding(fragment, launcher));
    }
  }

  @Override
  public synchronized void unbind(PlatformPermission permission, Fragment fragment) {
    removeBinding(permission, fragment);
  }

  @Override
  public synchronized void request(PlatformPermission permission, Consumer<Boolean> result) {
    Binding binding = activeBinding(permission);
    if (binding == null) {
      throw new IllegalStateException("No launcher bound for " + permission);
    }
    binding.callback = result;
    binding.launcher.launch(AndroidPermissionNames.platformName(permission));
  }

  private void consumeResult(PlatformPermission permission, Fragment fragment, boolean granted) {
    Consumer<Boolean> callback;
    synchronized (this) {
      Binding binding = findBinding(permission, fragment);
      if (binding == null) {
        return;
      }
      callback = binding.callback;
      binding.callback = null;
    }
    if (callback != null) {
      callback.accept(granted);
    }
  }

  private void removeBinding(PlatformPermission permission, Fragment fragment) {
    List<Binding> candidates = bindings.get(permission);
    if (candidates != null) {
      candidates.removeIf(binding -> binding.fragment == fragment);
      if (candidates.isEmpty()) {
        bindings.remove(permission);
      }
    }
  }

  private Binding findBinding(PlatformPermission permission, Fragment fragment) {
    List<Binding> candidates = bindings.get(permission);
    if (candidates == null) {
      return null;
    }
    for (Binding binding : candidates) {
      if (binding.fragment == fragment) {
        return binding;
      }
    }
    return null;
  }

  private Binding activeBinding(PlatformPermission permission) {
    List<Binding> candidates = bindings.get(permission);
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }
    for (Binding binding : candidates) {
      if (binding.fragment.isResumed()) {
        return binding;
      }
    }
    return candidates.get(candidates.size() - 1);
  }

  private static final class Binding {
    private final Fragment fragment;
    private final ActivityResultLauncher<String> launcher;
    private Consumer<Boolean> callback;

    private Binding(Fragment fragment, ActivityResultLauncher<String> launcher) {
      this.fragment = fragment;
      this.launcher = launcher;
    }
  }
}
