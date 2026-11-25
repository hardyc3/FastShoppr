/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.fastshoppr.app.domain.model.PlatformPermission;
import com.fastshoppr.app.domain.port.PermissionStatus;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Android-backed permission readout behind the domain port. */
@Singleton
public final class AndroidPermissionStatus implements PermissionStatus {

  private final Context context;
  private final AtomicReference<Activity> resumedActivity = new AtomicReference<>();
  private final AtomicReference<Activity> lastKnownActivity = new AtomicReference<>();

  @Inject
  public AndroidPermissionStatus(@ApplicationContext Context context) {
    this.context = context;
    Application application = (Application) context.getApplicationContext();
    application.registerActivityLifecycleCallbacks(
        new Application.ActivityLifecycleCallbacks() {
          @Override
          public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            lastKnownActivity.set(activity);
          }

          @Override
          public void onActivityStarted(Activity activity) {
            lastKnownActivity.set(activity);
          }

          @Override
          public void onActivityResumed(Activity activity) {
            resumedActivity.set(activity);
            lastKnownActivity.set(activity);
          }

          @Override
          public void onActivityPaused(Activity activity) {
            resumedActivity.compareAndSet(activity, null);
          }

          @Override
          public void onActivityStopped(Activity activity) {
            resumedActivity.compareAndSet(activity, null);
          }

          @Override
          public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

          @Override
          public void onActivityDestroyed(Activity activity) {
            resumedActivity.compareAndSet(activity, null);
            lastKnownActivity.compareAndSet(activity, null);
          }
        });
  }

  @Override
  public boolean isGranted(PlatformPermission permission) {
    try {
      return ContextCompat.checkSelfPermission(
              context, AndroidPermissionNames.platformName(permission))
          == PackageManager.PERMISSION_GRANTED;
    } catch (RuntimeException error) {
      return false;
    }
  }

  @Override
  public boolean shouldShowRationale(PlatformPermission permission) {
    Activity activity = resumedActivity.get();
    if (activity == null) {
      activity = lastKnownActivity.get();
    }
    if (activity == null) {
      return false;
    }
    try {
      return ActivityCompat.shouldShowRequestPermissionRationale(
          activity, AndroidPermissionNames.platformName(permission));
    } catch (RuntimeException error) {
      return false;
    }
  }
}
