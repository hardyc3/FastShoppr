/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.RecognizedLine;
import java.util.List;
import org.junit.Test;

public final class CaptureRepositoryImplTest {

  private final CaptureRepositoryImpl repository = new CaptureRepositoryImpl(null, null);

  @Test
  public void recognizedLinesRoundTripPreservesTextAndConfidence() {
    List<RecognizedLine> lines =
        List.of(new RecognizedLine("milk", 0.95f), new RecognizedLine("eggs", 0.2f));

    String json = repository.encodeRecognizedLines(lines);
    List<RecognizedLine> decoded = repository.decodeRecognizedLines(json);

    assertEquals(2, decoded.size());
    assertEquals("milk", decoded.get(0).getText());
    assertEquals(0.95f, decoded.get(0).getConfidence(), 0.0001f);
    assertEquals("eggs", decoded.get(1).getText());
    assertEquals(0.2f, decoded.get(1).getConfidence(), 0.0001f);
  }

  @Test
  public void encodeNullOrEmptyYieldsEmptyArray() {
    assertEquals("[]", repository.encodeRecognizedLines(null));
    assertEquals("[]", repository.encodeRecognizedLines(List.of()));
  }

  @Test
  public void decodeMalformedOrNullYieldsEmptyList() {
    assertTrue(repository.decodeRecognizedLines(null).isEmpty());
    assertTrue(repository.decodeRecognizedLines("").isEmpty());
    assertTrue(repository.decodeRecognizedLines("{not json").isEmpty());
    assertTrue(repository.decodeRecognizedLines("[{\"bogus\":").isEmpty());
  }
}
