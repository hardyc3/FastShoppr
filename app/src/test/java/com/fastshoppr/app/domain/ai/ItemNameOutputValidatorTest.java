/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.CleanItemResult;
import com.google.gson.Gson;
import org.junit.Test;

public final class ItemNameOutputValidatorTest {

  private final ItemNameOutputValidator validator = new ItemNameOutputValidator(new Gson());

  @Test
  public void acceptsValidJsonInsideCodeFence() {
    CleanItemResult result =
        validator.validate(
            "tp",
            "```json\n{\"displayName\":\"Toilet paper\",\"confidence\":1.4,\"reason\":\"Expanded\"}\n```");

    CleanItemResult.Suggested suggested = assertType(CleanItemResult.Suggested.class, result);
    assertEquals("Toilet paper", suggested.displayName());
    assertEquals(1f, suggested.confidence(), 0.0001f);
    assertEquals("Expanded", suggested.reason());
  }

  @Test
  public void noChangeWhenNameMatchesInputIgnoringCase() {
    CleanItemResult result =
        validator.validate("Milk", "{\"displayName\":\"milk\",\"confidence\":0.9}");

    assertType(CleanItemResult.NoChange.class, result);
  }

  @Test
  public void missingConfidenceAndReasonAreAllowed() {
    CleanItemResult result = validator.validate("tp", "{\"displayName\":\"Toilet paper\"}");

    CleanItemResult.Suggested suggested = assertType(CleanItemResult.Suggested.class, result);
    assertEquals(0f, suggested.confidence(), 0.0001f);
    assertNull(suggested.reason());
  }

  @Test
  public void rejectsMalformedOutput() {
    assertType(CleanItemResult.Unavailable.class, validator.validate("milk", "not json"));
    assertType(
        CleanItemResult.Unavailable.class, validator.validate("milk", "{\"displayName\": }"));
    assertType(CleanItemResult.Unavailable.class, validator.validate("milk", null));
    assertType(CleanItemResult.Unavailable.class, validator.validate("milk", ""));
  }

  @Test
  public void rejectsMissingOrBlankName() {
    assertType(
        CleanItemResult.Unavailable.class, validator.validate("milk", "{\"confidence\":0.5}"));
    assertType(
        CleanItemResult.Unavailable.class, validator.validate("milk", "{\"displayName\":\"   \"}"));
  }

  @Test
  public void rejectsUrls() {
    assertType(
        CleanItemResult.Unavailable.class,
        validator.validate("milk", "{\"displayName\":\"milk https://example.com\"}"));
    assertType(
        CleanItemResult.Unavailable.class,
        validator.validate("milk", "{\"displayName\":\"www.example.com milk\"}"));
  }

  @Test
  public void rejectsOverlongOutput() {
    String longName = "m".repeat(201);
    assertType(
        CleanItemResult.Unavailable.class,
        validator.validate("milk", "{\"displayName\":\"" + longName + "\",\"confidence\":0.5}"));
  }

  @Test
  public void rejectsChangedNumbersOrSizes() {
    assertType(
        CleanItemResult.Unavailable.class,
        validator.validate("2 lb beef", "{\"displayName\":\"3 lb beef\",\"confidence\":0.5}"));
    assertType(
        CleanItemResult.Unavailable.class,
        validator.validate("milk 2%", "{\"displayName\":\"milk\",\"confidence\":0.5}"));
  }

  @Test
  public void acceptsFormattingOnlyNumberChanges() {
    CleanItemResult result =
        validator.validate("milk 2,5", "{\"displayName\":\"Milk 2.5\",\"confidence\":0.6}");

    assertType(CleanItemResult.Suggested.class, result);
  }

  @Test
  public void trimsLongReason() {
    String longReason = "r".repeat(250);
    CleanItemResult result =
        validator.validate(
            "tp",
            "{\"displayName\":\"Toilet paper\",\"confidence\":0.5,\"reason\":\""
                + longReason
                + "\"}");

    CleanItemResult.Suggested suggested = assertType(CleanItemResult.Suggested.class, result);
    assertEquals(200, suggested.reason().length());
  }

  private static <T> T assertType(Class<T> type, Object value) {
    assertTrue("expected " + type.getSimpleName(), type.isInstance(value));
    return type.cast(value);
  }
}
