/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class TextNormalizerTest {

  @Test
  public void collapsesWhitespaceAndTrims() {
    assertEquals("two percent milk", TextNormalizer.collapseWhitespace("  two   percent\tmilk  "));
    assertNull(TextNormalizer.trimToNull("   "));
    assertEquals("milk", TextNormalizer.trimToNull(" milk "));
  }

  @Test
  public void normalizesForLookup() {
    assertEquals("milk", TextNormalizer.normalizeForLookup("  MILK!!  "));
    assertEquals("paper towels", TextNormalizer.normalizeForLookup("Paper-Towels"));
    assertEquals("milk", TextNormalizer.normalizeForLookup("2. Milk"));
    assertEquals("eggs", TextNormalizer.normalizeForLookup("- eggs"));
    assertEquals("", TextNormalizer.normalizeForLookup(null));
  }
}
