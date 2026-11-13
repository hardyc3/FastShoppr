/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.CleanItemResult;
import org.junit.Test;

public final class RuleBasedItemNameCleanerTest {

  private final RuleBasedItemNameCleaner cleaner = new RuleBasedItemNameCleaner();

  @Test
  public void collapsesWhitespaceAndCapitalizes() {
    CleanItemResult result = cleaner.cleanText("  paper   towels ");

    CleanItemResult.Suggested suggested = assertType(CleanItemResult.Suggested.class, result);
    assertEquals("Paper towels", suggested.displayName());
    assertEquals(RuleBasedItemNameCleaner.CONFIDENCE, suggested.confidence(), 0.0001f);
  }

  @Test
  public void removesListMarkers() {
    assertEquals("Milk", suggested(cleaner.cleanText("- milk")).displayName());
    assertEquals("Eggs", suggested(cleaner.cleanText("1. eggs")).displayName());
  }

  @Test
  public void expandsDocumentedAbbreviations() {
    assertEquals("Toilet paper", suggested(cleaner.cleanText("tp")).displayName());
    assertEquals("Peanut butter", suggested(cleaner.cleanText("pb")).displayName());
    assertEquals("Gallon", suggested(cleaner.cleanText("gal")).displayName());
  }

  @Test
  public void expandsAbbreviationsInsideSentences() {
    assertEquals("2 gallon paint", suggested(cleaner.cleanText("2 gal paint")).displayName());
  }

  @Test
  public void leavesCleanTextUnchanged() {
    assertType(CleanItemResult.NoChange.class, cleaner.cleanText("Milk"));
    assertType(CleanItemResult.NoChange.class, cleaner.cleanText("2 apples"));
  }

  @Test
  public void blankTextIsNoChange() {
    assertType(CleanItemResult.NoChange.class, cleaner.cleanText(""));
    assertType(CleanItemResult.NoChange.class, cleaner.cleanText(null));
  }

  @Test
  public void neverChangesNumbersOrUnits() {
    assertEquals("2 lb beef", suggested(cleaner.cleanText("- 2 lb beef")).displayName());
  }

  private static CleanItemResult.Suggested suggested(CleanItemResult result) {
    return (CleanItemResult.Suggested) result;
  }

  private static <T> T assertType(Class<T> type, Object value) {
    assertTrue("expected " + type.getSimpleName(), type.isInstance(value));
    return type.cast(value);
  }
}
