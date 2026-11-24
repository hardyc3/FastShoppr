/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public final class CapturedLinesCodecTest {

  private final CapturedLinesCodec codec = new CapturedLinesCodec(new Gson());

  @Test
  public void roundTripPreservesEveryReviewField() {
    List<CapturedLine> lines =
        List.of(
            new CapturedLine("milk", "Organic milk", false, 0.95f, true, "store-1", "2026-11-08"),
            new CapturedLine("eggs", null, true, 0.2f, false, null, null));

    List<CapturedLine> decoded = codec.decode(codec.encode(lines));

    assertEquals(2, decoded.size());
    assertEquals("milk", decoded.get(0).getText());
    assertEquals("Organic milk", decoded.get(0).getDisplayName());
    assertFalse(decoded.get(0).isSelected());
    assertEquals(0.95f, decoded.get(0).getConfidence(), 0.0001f);
    assertTrue(decoded.get(0).isManual());
    assertEquals("store-1", decoded.get(0).getStoreId());
    assertEquals("2026-11-08", decoded.get(0).getTargetDate());
    assertEquals("eggs", decoded.get(1).getText());
    assertNull(decoded.get(1).getDisplayName());
    assertTrue(decoded.get(1).isSelected());
    assertNull(decoded.get(1).getStoreId());
    assertNull(decoded.get(1).getTargetDate());
  }

  @Test
  public void decodesLegacyRecognizedLineJson() {
    String legacy =
        "[{\"text\":\"milk\",\"confidence\":0.87}," + "{\"text\":\"eggs\",\"confidence\":0.15}]";

    List<CapturedLine> decoded = codec.decode(legacy);

    assertEquals(2, decoded.size());
    assertEquals("milk", decoded.get(0).getText());
    assertNull(decoded.get(0).getDisplayName());
    assertTrue(decoded.get(0).isSelected());
    assertEquals(0.87f, decoded.get(0).getConfidence(), 0.0001f);
    assertFalse(decoded.get(0).isManual());
    assertNull(decoded.get(0).getStoreId());
    assertEquals("eggs", decoded.get(1).getText());
  }

  @Test
  public void missingFieldsUseSafeDefaults() {
    List<CapturedLine> decoded = codec.decode("[{\"text\":\"milk\"}]");

    assertEquals(1, decoded.size());
    assertNull(decoded.get(0).getDisplayName());
    assertTrue(decoded.get(0).isSelected());
    assertEquals(RecognizedLine.UNKNOWN_CONFIDENCE, decoded.get(0).getConfidence(), 0.0001f);
    assertFalse(decoded.get(0).isManual());
  }

  @Test
  public void encodeNullOrEmptyYieldsEmptyArray() {
    assertEquals("[]", codec.encode(null));
    assertEquals("[]", codec.encode(List.of()));
  }

  @Test
  public void malformedInputYieldsEmptyList() {
    assertTrue(codec.decode(null).isEmpty());
    assertTrue(codec.decode("").isEmpty());
    assertTrue(codec.decode("   ").isEmpty());
    assertTrue(codec.decode("{not json").isEmpty());
    assertTrue(codec.decode("{\"text\":\"milk\"}").isEmpty());
    assertTrue(codec.decode("[null, 3, {\"other\":\"value\"}]").isEmpty());
  }
}
