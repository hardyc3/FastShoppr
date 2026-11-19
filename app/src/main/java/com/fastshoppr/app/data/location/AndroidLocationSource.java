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
import androidx.core.content.ContextCompat;
import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.repository.LocationSource;
import com.google.android.gms.location.LocationServices;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Reads a recent coarse location fix only after the user grants permission. */
@Singleton
public final class AndroidLocationSource implements LocationSource {

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
      LocationServices.getFusedLocationProviderClient(context)
          .getLastLocation()
          .addOnSuccessListener(location -> result.complete(toGeoPoint(location)))
          .addOnFailureListener(result::completeExceptionally);
    } catch (SecurityException denied) {
      result.complete(null);
    }
    return result;
  }

  private static GeoPoint toGeoPoint(Location location) {
    return location == null ? null : new GeoPoint(location.getLatitude(), location.getLongitude());
  }
}
