/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.data.preferences.SettingsPreferences;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public final class SelectedStoreRepositoryImplTest {

  private final Map<String, String> values = new HashMap<>();
  private final SettingsPreferences preferences = mock(SettingsPreferences.class);
  private SelectedStoreRepositoryImpl repository;

  @Before
  public void setUp() {
    when(preferences.getSelectedStoreJson(any()))
        .thenAnswer(
            call -> values.get("selected_store_" + ((StoreBrand) call.getArgument(0)).name()));
    doAnswer(
            call -> {
              values.put(
                  "selected_store_" + ((StoreBrand) call.getArgument(0)).name(),
                  call.getArgument(1));
              return null;
            })
        .when(preferences)
        .setSelectedStoreJson(any(), any());
    when(preferences.getStoreLocatorPostalCode()).thenAnswer(call -> values.get("zip"));
    doAnswer(
            call -> {
              values.put("zip", call.getArgument(0));
              return null;
            })
        .when(preferences)
        .setStoreLocatorPostalCode(any());
    repository = new SelectedStoreRepositoryImpl(preferences, new Gson());
  }

  @Test
  public void savesAndReadsSelectedStorePerBrand() {
    NearbyStore store =
        new NearbyStore(
            "642",
            StoreBrand.HOME_DEPOT,
            "Home Depot #642",
            "100 Market St",
            "San Francisco",
            "CA",
            "94103",
            37.77,
            -122.41,
            NearbyStoreSource.LOCATOR);

    repository.select(store);

    assertEquals(store.getName(), repository.getSelected(StoreBrand.HOME_DEPOT).getName());
    assertNull(repository.getSelected(StoreBrand.HOME_DEPOT).getLatitude());
    assertNull(repository.getSelected(StoreBrand.HOME_DEPOT).getLongitude());
    String storedJson = values.get("selected_store_" + StoreBrand.HOME_DEPOT.name());
    assertFalse(storedJson.contains("latitude"));
    assertFalse(storedJson.contains("longitude"));
    assertNull(repository.getSelected(StoreBrand.LOWES));
    assertEquals(1, repository.getSelected().size());
  }

  @Test
  public void clearRemovesOnlyThatBrand() {
    repository.select(store(StoreBrand.WALMART));
    repository.select(store(StoreBrand.TARGET));

    repository.clear(StoreBrand.WALMART);

    assertNull(repository.getSelected(StoreBrand.WALMART));
    assertEquals(StoreBrand.TARGET, repository.getSelected().keySet().iterator().next());
  }

  @Test
  public void malformedStoredJsonIsIgnored() {
    values.put("selected_store_" + StoreBrand.LOWES.name(), "{not-json");

    assertNull(repository.getSelected(StoreBrand.LOWES));
    assertTrue(repository.getSelected().isEmpty());
  }

  @Test
  public void postalCodeIsTrimmed() {
    repository.setPostalCode("  90210  ");

    assertEquals("90210", repository.getPostalCode());
  }

  private static NearbyStore store(StoreBrand brand) {
    return new NearbyStore(
        brand.name(),
        brand,
        brand.getDisplayName(),
        null,
        null,
        null,
        null,
        null,
        null,
        NearbyStoreSource.MANUAL);
  }
}
