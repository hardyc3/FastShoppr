/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.util.Objects;

/** One recognized text line with its confidence score. */
public final class RecognizedLine {

  /** Confidence values below this marker mean "unknown". */
  public static final float UNKNOWN_CONFIDENCE = -1.0f;

  private final String text;
  private final float confidence;

  public RecognizedLine(String text, float confidence) {
    this.text = Objects.requireNonNull(text, "text must not be null");
    this.confidence = validate(confidence);
  }

  private static float validate(float value) {
    if (Float.isNaN(value)) {
      return UNKNOWN_CONFIDENCE;
    }
    if (value < 0.0f || value > 1.0f) {
      if (value == UNKNOWN_CONFIDENCE) {
        return UNKNOWN_CONFIDENCE;
      }
      return Math.max(0.0f, Math.min(1.0f, value));
    }
    return value;
  }

  public String getText() {
    return text;
  }

  public float getConfidence() {
    return confidence;
  }
}
