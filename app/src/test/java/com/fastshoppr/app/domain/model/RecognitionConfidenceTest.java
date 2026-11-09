/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class RecognitionConfidenceTest {

  @Test
  public void inkConfidenceMustExceedEightyPercent() {
    assertTrue(RecognitionConfidence.isLow(0.8f, ItemSource.DIGITAL_INK));
    assertFalse(RecognitionConfidence.isLow(0.81f, ItemSource.DIGITAL_INK));
  }

  @Test
  public void ocrConfidenceMustExceedSeventyPercent() {
    assertTrue(RecognitionConfidence.isLow(0.7f, ItemSource.CAMERA_OCR));
    assertTrue(RecognitionConfidence.isLow(0.7f, ItemSource.GALLERY_OCR));
    assertFalse(RecognitionConfidence.isLow(0.71f, ItemSource.CAMERA_OCR));
  }

  @Test
  public void unknownConfidenceCountsAsLow() {
    assertTrue(
        RecognitionConfidence.isLow(RecognizedLine.UNKNOWN_CONFIDENCE, ItemSource.DIGITAL_INK));
    assertTrue(RecognitionConfidence.isLow(-1f, ItemSource.CAMERA_OCR));
  }

  @Test
  public void recognizedLineClampsOutOfRangeConfidence() {
    assertEquals(1.0f, new RecognizedLine("milk", 1.7f).getConfidence(), 0.0001f);
    assertEquals(0.0f, new RecognizedLine("milk", -0.5f).getConfidence(), 0.0001f);
    assertEquals(
        RecognizedLine.UNKNOWN_CONFIDENCE,
        new RecognizedLine("milk", Float.NaN).getConfidence(),
        0.0001f);
    assertEquals(
        RecognizedLine.UNKNOWN_CONFIDENCE,
        new RecognizedLine("milk", -1f).getConfidence(),
        0.0001f);
  }

  @Test
  public void inkDataKeepsStrokesInOrder() {
    InkStroke first =
        new InkStroke(List.of(new InkPoint(1f, 2f, 100L), new InkPoint(3f, 4f, 200L)));
    InkStroke second = new InkStroke(List.of(new InkPoint(5f, 6f, 300L)));

    InkData data = new InkData(100, 200, List.of(first, second));

    assertEquals(2, data.getStrokes().size());
    assertEquals(100L, data.getStrokes().get(0).getPoints().get(0).getTimestampMillis());
    assertFalse(data.isEmpty());
    assertTrue(new InkData(100, 200, List.of()).isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyStrokeIsRejected() {
    new InkStroke(List.of());
  }
}
