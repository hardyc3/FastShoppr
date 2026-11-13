/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.LookupSource;
import java.util.List;
import org.junit.Test;

public class LookupCandidatesCodecTest {

  @Test
  public void roundTrip() {
    List<StoreLookupResult.Found> candidates =
        List.of(
            new StoreLookupResult.Found(
                "A12",
                "003",
                "Milk",
                "https://www.walmart.com/ip/milk/1",
                0.7f,
                LookupSource.WALMART_WEBVIEW),
            new StoreLookupResult.Found(
                "A12", "004", "2% Milk", null, 0.5f, LookupSource.WALMART_WEBVIEW));

    String json = LookupCandidatesCodec.encode(candidates);
    List<StoreLookupResult.Found> decoded = LookupCandidatesCodec.decode(json);

    assertEquals(2, decoded.size());
    assertEquals("A12", decoded.get(0).getAisle());
    assertEquals("003", decoded.get(0).getBay());
    assertEquals("Milk", decoded.get(0).getProductName());
    assertEquals("https://www.walmart.com/ip/milk/1", decoded.get(0).getProductUrl());
    assertEquals(Float.valueOf(0.7f), decoded.get(0).getConfidence());
    assertEquals(LookupSource.WALMART_WEBVIEW, decoded.get(0).getSource());
    assertEquals("004", decoded.get(1).getBay());
  }

  @Test
  public void encodeEmptyReturnsNull() {
    assertEquals(null, LookupCandidatesCodec.encode(List.of()));
  }

  @Test
  public void decodeGarbageReturnsEmptyList() {
    assertTrue(LookupCandidatesCodec.decode("not json").isEmpty());
    assertTrue(LookupCandidatesCodec.decode(null).isEmpty());
    assertTrue(LookupCandidatesCodec.decode("{}").isEmpty());
  }
}
