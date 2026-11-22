/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import com.fastshoppr.app.data.preferences.SettingsPreferences;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SelectedStoreRepositoryImpl implements SelectedStoreRepository {

  private final SettingsPreferences preferences;
  private final Gson gson;

  @Inject
  public SelectedStoreRepositoryImpl(SettingsPreferences preferences, Gson gson) {
    this.preferences = preferences;
    this.gson = gson;
  }

  @Override
  public NearbyStore getSelected(StoreBrand brand) {
    String json = preferences.getSelectedStoreJson(brand);
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      NearbyStore stored = gson.fromJson(json, NearbyStore.class);
      return stored == null ? null : withoutPostalCode(stored);
    } catch (JsonParseException | IllegalArgumentException malformed) {
      return null;
    }
  }

  @Override
  public Map<StoreBrand, NearbyStore> getSelected() {
    Map<StoreBrand, NearbyStore> selected = new EnumMap<>(StoreBrand.class);
    for (StoreBrand brand : StoreBrand.values()) {
      if (brand == StoreBrand.CUSTOM) {
        continue;
      }
      NearbyStore store = getSelected(brand);
      if (store != null) {
        selected.put(brand, store);
      }
    }
    return Collections.unmodifiableMap(selected);
  }

  @Override
  public void select(NearbyStore store) {
    NearbyStore sanitized = withoutPostalCode(store);
    preferences.setSelectedStoreJson(sanitized.getBrand(), gson.toJson(sanitized));
  }

  @Override
  public void clear(StoreBrand brand) {
    preferences.setSelectedStoreJson(brand, null);
  }

  /** Drops ZIP/coordinate values so only branch identity and street/city/state are persisted. */
  private static NearbyStore withoutPostalCode(NearbyStore store) {
    return new NearbyStore(
        store.getId(),
        store.getBrand(),
        store.getName(),
        store.getAddressLine(),
        store.getCity(),
        store.getState(),
        null,
        null,
        null,
        store.getSource());
  }
}
