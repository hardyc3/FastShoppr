/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.time.Clock;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;

public final class StoreLocatorCacheTest {

  private final Map<String, String> values = new HashMap<>();
  private final AtomicLong now = new AtomicLong(1_000L);
  private final SharedPreferences preferences = mock(SharedPreferences.class);
  private StoreLocatorCache cache;

  @Before
  public void setUp() {
    SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
    when(preferences.edit()).thenReturn(editor);
    when(preferences.getString(anyString(), any()))
        .thenAnswer(call -> values.get(call.getArgument(0)));
    when(editor.putString(anyString(), any()))
        .thenAnswer(
            call -> {
              values.put(call.getArgument(0), call.getArgument(1));
              return editor;
            });
    doAnswer(
            call -> {
              values.remove(call.getArgument(0));
              return editor;
            })
        .when(editor)
        .remove(anyString());
    doAnswer(
            call -> {
              values.clear();
              return editor;
            })
        .when(editor)
        .clear();
    when(editor.commit()).thenReturn(true);
    cache = new StoreLocatorCache(preferences, new Gson(), (Clock) now::get);
  }

  @Test
  public void cachedStoresDoNotPersistCoordinates() {
    cache.put("walmart:zip:90210", List.of(storeWithCoordinates()));

    String raw = values.get("walmart:zip:90210");
    assertFalse(raw.contains("latitude"));
    assertFalse(raw.contains("longitude"));
    NearbyStore cached = cache.get("walmart:zip:90210").get(0);
    assertNull(cached.getLatitude());
    assertNull(cached.getLongitude());
  }

  @Test
  public void expiredEntryIsRemoved() {
    cache.put("walmart:zip:90210", List.of(storeWithCoordinates()));
    now.set(1_000L + StoreLocatorCache.TTL_MILLIS + 1L);

    assertNull(cache.get("walmart:zip:90210"));
    assertFalse(values.containsKey("walmart:zip:90210"));
  }

  @Test
  public void clearRemovesEveryEntry() {
    cache.put("walmart:zip:90210", List.of(storeWithCoordinates()));
    cache.put("target:zip:90210", List.of(storeWithCoordinates()));

    cache.clear();

    assertEquals(Map.of(), values);
  }

  private static NearbyStore storeWithCoordinates() {
    return new NearbyStore(
        "123",
        StoreBrand.WALMART,
        "Walmart Supercenter",
        "1 Main St",
        "Springfield",
        "IL",
        "62701",
        39.8,
        -89.6,
        NearbyStoreSource.LOCATOR);
  }
}
