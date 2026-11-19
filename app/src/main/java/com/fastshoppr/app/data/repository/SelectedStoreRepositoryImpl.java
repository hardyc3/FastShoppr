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
      return gson.fromJson(json, NearbyStore.class);
    } catch (JsonParseException malformed) {
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
    preferences.setSelectedStoreJson(store.getBrand(), gson.toJson(store));
  }

  @Override
  public void clear(StoreBrand brand) {
    preferences.setSelectedStoreJson(brand, null);
  }

  @Override
  public String getPostalCode() {
    return preferences.getStoreLocatorPostalCode();
  }

  @Override
  public void setPostalCode(String postalCode) {
    preferences.setStoreLocatorPostalCode(
        postalCode == null || postalCode.isBlank() ? null : postalCode.trim());
  }
}
