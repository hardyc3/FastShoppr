/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
    repository = new SelectedStoreRepositoryImpl(preferences, new Gson());
  }

  @Test
  public void savesAndReadsOneBranchPerBrandAsJson() {
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

    NearbyStore restored = repository.getSelected(StoreBrand.HOME_DEPOT);
    assertEquals(store.getName(), restored.getName());
    assertEquals("100 Market St", restored.getAddressLine());
    assertNull(restored.getPostalCode());
    assertEquals(NearbyStoreSource.LOCATOR, restored.getSource());
    assertNull(restored.getLatitude());
    assertNull(restored.getLongitude());
    String storedJson = values.get("selected_store_" + StoreBrand.HOME_DEPOT.name());
    assertTrue(storedJson.contains("\"name\":\"Home Depot #642\""));
    assertFalse(storedJson.contains("postalCode"));
    assertFalse(storedJson.contains("94103"));
    assertFalse(storedJson.contains("latitude"));
    assertFalse(storedJson.contains("longitude"));
    assertNull(repository.getSelected(StoreBrand.LOWES));
    assertEquals(1, repository.getSelected().size());
  }

  @Test
  public void selectingSameBrandReplacesPreviousBranch() {
    repository.select(store(StoreBrand.WALMART, "Walmart #1"));
    repository.select(store(StoreBrand.WALMART, "Walmart #2"));

    assertEquals("Walmart #2", repository.getSelected(StoreBrand.WALMART).getName());
    assertEquals(1, repository.getSelected().size());
    assertEquals(1, values.size());
  }

  @Test
  public void multipleBrandsAreSavedUnderSeparateKeys() {
    repository.select(store(StoreBrand.WALMART, "Walmart #1"));
    repository.select(store(StoreBrand.TARGET, "Target #2"));

    assertEquals(2, repository.getSelected().size());
    assertEquals("Walmart #1", repository.getSelected(StoreBrand.WALMART).getName());
    assertEquals("Target #2", repository.getSelected(StoreBrand.TARGET).getName());
    assertTrue(values.containsKey("selected_store_WALMART"));
    assertTrue(values.containsKey("selected_store_TARGET"));
  }

  @Test
  public void clearRemovesOnlyThatBrand() {
    repository.select(store(StoreBrand.WALMART, "Walmart #1"));
    repository.select(store(StoreBrand.TARGET, "Target #2"));

    repository.clear(StoreBrand.WALMART);

    assertNull(repository.getSelected(StoreBrand.WALMART));
    assertEquals(StoreBrand.TARGET, repository.getSelected().keySet().iterator().next());
  }

  @Test
  public void legacyStoredJsonWithZipAndCoordinatesIsSanitizedOnRead() {
    NearbyStore legacy =
        new NearbyStore(
            "99",
            StoreBrand.LOWES,
            "Lowe's #99",
            "9 Pine St",
            "Austin",
            "TX",
            "78701",
            30.27,
            -97.74,
            NearbyStoreSource.LOCATOR);
    values.put("selected_store_LOWES", new Gson().toJson(legacy));

    NearbyStore restored = repository.getSelected(StoreBrand.LOWES);

    assertNotNull(restored);
    assertEquals("Lowe's #99", restored.getName());
    assertNull(restored.getPostalCode());
    assertNull(restored.getLatitude());
    assertNull(restored.getLongitude());
  }

  @Test
  public void malformedStoredJsonIsIgnored() {
    values.put("selected_store_" + StoreBrand.LOWES.name(), "{not-json");
    values.put("selected_store_" + StoreBrand.SMITHS.name(), "\"not-an-object\"");
    values.put("selected_store_" + StoreBrand.TARGET.name(), "   ");

    assertNull(repository.getSelected(StoreBrand.LOWES));
    assertNull(repository.getSelected(StoreBrand.SMITHS));
    assertNull(repository.getSelected(StoreBrand.TARGET));
    assertTrue(repository.getSelected().isEmpty());
  }

  @Test
  public void customBrandIsNotPartOfAggregateBranches() {
    repository.select(store(StoreBrand.CUSTOM, "My shop"));

    assertEquals("My shop", repository.getSelected(StoreBrand.CUSTOM).getName());
    assertTrue(repository.getSelected().isEmpty());
    assertTrue(values.containsKey("selected_store_CUSTOM"));
  }

  private static NearbyStore store(StoreBrand brand, String name) {
    return new NearbyStore(
        brand.name(), brand, name, null, null, null, null, null, null, NearbyStoreSource.LOCATOR);
  }
}
