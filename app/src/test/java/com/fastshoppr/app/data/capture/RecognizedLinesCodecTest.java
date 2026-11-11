/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.RecognizedLine;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public final class RecognizedLinesCodecTest {

  private final RecognizedLinesCodec codec = new RecognizedLinesCodec(new Gson());

  @Test
  public void roundTripPreservesTextAndConfidence() {
    List<RecognizedLine> lines =
        List.of(new RecognizedLine("milk", 0.95f), new RecognizedLine("eggs", 0.2f));

    List<RecognizedLine> decoded = codec.decode(codec.encode(lines));

    assertEquals(2, decoded.size());
    assertEquals("milk", decoded.get(0).getText());
    assertEquals(0.95f, decoded.get(0).getConfidence(), 0.0001f);
    assertEquals("eggs", decoded.get(1).getText());
    assertEquals(0.2f, decoded.get(1).getConfidence(), 0.0001f);
  }

  @Test
  public void encodeNullOrEmptyYieldsEmptyArray() {
    assertEquals("[]", codec.encode(null));
    assertEquals("[]", codec.encode(List.of()));
  }

  @Test
  public void decodeMalformedOrNullYieldsEmptyList() {
    assertTrue(codec.decode(null).isEmpty());
    assertTrue(codec.decode("").isEmpty());
    assertTrue(codec.decode("{not json").isEmpty());
    assertTrue(codec.decode("[{\"bogus\":").isEmpty());
  }
}
