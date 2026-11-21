/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import android.content.Context;
import android.content.SharedPreferences;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.time.Clock;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Local, 60-day cache for public store-locator responses. */
@Singleton
public final class StoreLocatorCache {

  static final long TTL_MILLIS = 60L * 24L * 60L * 60L * 1_000L;
  private static final String PREFS_NAME = "fastshoppr_store_locator_cache";

  private final SharedPreferences preferences;
  private final Gson gson;
  private final Clock clock;

  @Inject
  public StoreLocatorCache(@ApplicationContext Context context, Gson gson, Clock clock) {
    this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE), gson, clock);
  }

  StoreLocatorCache(SharedPreferences preferences, Gson gson, Clock clock) {
    this.preferences = preferences;
    this.gson = gson;
    this.clock = clock;
  }

  List<NearbyStore> get(String key) {
    String raw = preferences.getString(key, null);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      JsonObject entry = JsonParser.parseString(raw).getAsJsonObject();
      long fetchedAt = entry.get("fetchedAt").getAsLong();
      if (clock.nowMillis() - fetchedAt > TTL_MILLIS) {
        preferences.edit().remove(key).apply();
        return null;
      }
      JsonArray stores = entry.getAsJsonArray("stores");
      return gson.fromJson(stores, new TypeToken<List<NearbyStore>>() {}.getType());
    } catch (JsonParseException | IllegalStateException | NullPointerException malformed) {
      preferences.edit().remove(key).apply();
      return null;
    }
  }

  void put(String key, List<NearbyStore> stores) {
    JsonObject entry = new JsonObject();
    entry.addProperty("fetchedAt", clock.nowMillis());
    entry.add("stores", gson.toJsonTree(stores));
    preferences.edit().putString(key, entry.toString()).apply();
  }

  public void clear() {
    preferences.edit().clear().commit();
  }
}
