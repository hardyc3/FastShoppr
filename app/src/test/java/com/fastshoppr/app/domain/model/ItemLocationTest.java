/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ItemLocationTest {

  @Test
  public void presentLocationMarksItemFoundFromManualEntry() {
    ItemLocation location = ItemLocation.of(" A12 ", " 3 ");

    assertEquals("A12", location.aisle());
    assertEquals("3", location.bay());
    assertTrue(location.isPresent());
    assertEquals(LookupStatus.FOUND, location.lookupStatus());
    assertEquals(LookupSource.MANUAL, location.lookupSource());
  }

  @Test
  public void emptyLocationReturnsItemToPending() {
    ItemLocation location = ItemLocation.of(" ", null);

    assertNull(location.aisle());
    assertNull(location.bay());
    assertFalse(location.isPresent());
    assertEquals(LookupStatus.PENDING, location.lookupStatus());
    assertNull(location.lookupSource());
  }
}
