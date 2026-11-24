/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.util.Objects;

/** Persisted review state for one captured line. Contains no Android types. */
public final class CapturedLine {

  private final String text;
  private final String displayName;
  private final boolean selected;
  private final float confidence;
  private final boolean manual;
  private final String storeId;
  private final String targetDate;

  public CapturedLine(
      String text,
      String displayName,
      boolean selected,
      float confidence,
      boolean manual,
      String storeId,
      String targetDate) {
    this.text = Objects.requireNonNull(text, "text must not be null");
    this.displayName = displayName;
    this.selected = selected;
    this.confidence = confidence;
    this.manual = manual;
    this.storeId = storeId;
    this.targetDate = targetDate;
  }

  public String getText() {
    return text;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isSelected() {
    return selected;
  }

  public float getConfidence() {
    return confidence;
  }

  public boolean isManual() {
    return manual;
  }

  public String getStoreId() {
    return storeId;
  }

  public String getTargetDate() {
    return targetDate;
  }
}
