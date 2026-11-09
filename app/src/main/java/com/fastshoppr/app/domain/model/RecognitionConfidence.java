/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** Confidence rules shared by the review screen and capture flows. */
public final class RecognitionConfidence {

  /** Digital ink confidence must be strictly above 80% to count as high. */
  public static final float INK_HIGH_THRESHOLD = 0.8f;

  /** Camera/gallery OCR confidence must be strictly above 70% to count as high. */
  public static final float OCR_HIGH_THRESHOLD = 0.7f;

  private RecognitionConfidence() {}

  public static float thresholdFor(ItemSource source) {
    return source == ItemSource.DIGITAL_INK ? INK_HIGH_THRESHOLD : OCR_HIGH_THRESHOLD;
  }

  /**
   * A line is low confidence when its score does not exceed the source threshold. Unknown
   * confidence counts as low so nothing is silently trusted.
   */
  public static boolean isLow(float confidence, ItemSource source) {
    return !(confidence > thresholdFor(source));
  }
}
