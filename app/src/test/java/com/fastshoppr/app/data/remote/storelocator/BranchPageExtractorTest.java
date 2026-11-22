/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.fastshoppr.app.domain.model.BranchPage;
import com.google.gson.Gson;
import org.junit.Test;

public final class BranchPageExtractorTest {

  private final BranchPageExtractor extractor = new BranchPageExtractor();
  private final Gson gson = new Gson();

  @Test
  public void parsesAllFieldsFromEvaluateJavascriptStringValue() {
    String json =
        "{\"name\":\"Walmart #123\",\"addressLine\":\"1 Main St\","
            + "\"city\":\"Springfield\",\"state\":\"IL\",\"postalCode\":\"62701\","
            + "\"title\":\"Walmart #123 - Walmart\",\"bodyText\":\"Store details\"}";

    BranchPage page = extractor.parse(gson.toJson(json));

    assertEquals(
        new BranchPage(
            "Walmart #123",
            "1 Main St",
            "Springfield",
            "IL",
            "62701",
            "Walmart #123 - Walmart",
            "Store details"),
        page);
  }

  @Test
  public void metaAndTitleFallbackFieldsCanBeEmpty() {
    String json =
        "{\"name\":\"Target #42\",\"addressLine\":\"\",\"city\":\"\",\"state\":\"\","
            + "\"postalCode\":\"\",\"title\":\"Target #42 | Target\",\"bodyText\":\"\"}";

    BranchPage page = extractor.parse(gson.toJson(json));

    assertEquals("Target #42", page.name());
    assertEquals("", page.addressLine());
    assertEquals("", page.city());
    assertEquals("", page.state());
    assertEquals("", page.postalCode());
    assertEquals("Target #42 | Target", page.title());
    assertEquals("", page.bodyText());
  }

  @Test
  public void trimsValuesAndTreatsNullsAsEmpty() {
    String json =
        "{\"name\":\"  Lowe's #7  \",\"addressLine\":null,\"city\":null,"
            + "\"state\":null,\"postalCode\":null,\"title\":null,\"bodyText\":null}";

    BranchPage page = extractor.parse(gson.toJson(json));

    assertEquals("Lowe's #7", page.name());
    assertEquals("", page.addressLine());
    assertEquals("", page.city());
    assertEquals("", page.state());
    assertEquals("", page.postalCode());
    assertEquals("", page.title());
    assertEquals("", page.bodyText());
  }

  @Test
  public void missingOrBlankNameIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> extractor.parse(gson.toJson("{\"addressLine\":\"1 Main St\"}")));
    assertThrows(
        IllegalArgumentException.class, () -> extractor.parse(gson.toJson("{\"name\":\"   \"}")));
  }

  @Test
  public void malformedExtractionDataIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> extractor.parse(null));
    assertThrows(IllegalArgumentException.class, () -> extractor.parse("null"));
    assertThrows(IllegalArgumentException.class, () -> extractor.parse("{}"));
    assertThrows(IllegalArgumentException.class, () -> extractor.parse(gson.toJson("{not json")));
    assertThrows(IllegalArgumentException.class, () -> extractor.parse(gson.toJson(42)));
  }
}
