/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.repository.LocationSource;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Reads a coarse device location only after the user grants permission. */
@Singleton
public final class AndroidLocationSource implements LocationSource {

  private static final long FIX_TIMEOUT_MILLIS = 8_000L;

  private final Context context;

  @Inject
  public AndroidLocationSource(@ApplicationContext Context context) {
    this.context = context;
  }

  @Override
  public CompletableFuture<GeoPoint> lastKnownLocation() {
    CompletableFuture<GeoPoint> result = new CompletableFuture<>();
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      result.complete(null);
      return result;
    }
    try {
      FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
      CancellationTokenSource cancellation = new CancellationTokenSource();
      Handler handler = new Handler(Looper.getMainLooper());
      Runnable timeout =
          () -> {
            cancellation.cancel();
            completeWithLastKnown(client, result);
          };
      handler.postDelayed(timeout, FIX_TIMEOUT_MILLIS);
      client
          .getCurrentLocation(Priority.PRIORITY_LOW_POWER, cancellation.getToken())
          .addOnSuccessListener(
              location -> {
                handler.removeCallbacks(timeout);
                if (location == null) {
                  completeWithLastKnown(client, result);
                } else {
                  result.complete(toGeoPoint(location));
                }
              })
          .addOnFailureListener(
              error -> {
                handler.removeCallbacks(timeout);
                completeWithLastKnown(client, result);
              });
    } catch (RuntimeException unavailable) {
      result.complete(null);
    }
    return result;
  }

  /** A fresh fix is not always available, so fall back to a recent cached one. */
  private static void completeWithLastKnown(
      FusedLocationProviderClient client, CompletableFuture<GeoPoint> result) {
    if (result.isDone()) {
      return;
    }
    try {
      client
          .getLastLocation()
          .addOnSuccessListener(location -> result.complete(toGeoPoint(location)))
          .addOnFailureListener(error -> result.complete(null));
    } catch (SecurityException denied) {
      result.complete(null);
    }
  }

  private static GeoPoint toGeoPoint(Location location) {
    return location == null ? null : new GeoPoint(location.getLatitude(), location.getLongitude());
  }
}
