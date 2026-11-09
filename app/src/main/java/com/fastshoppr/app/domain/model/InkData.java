/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ink strokes captured on the writing canvas, with the canvas size in pixels. */
public final class InkData {

  private final int canvasWidth;
  private final int canvasHeight;
  private final List<InkStroke> strokes;

  public InkData(int canvasWidth, int canvasHeight, List<InkStroke> strokes) {
    this.canvasWidth = canvasWidth;
    this.canvasHeight = canvasHeight;
    this.strokes =
        strokes == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(strokes));
  }

  public int getCanvasWidth() {
    return canvasWidth;
  }

  public int getCanvasHeight() {
    return canvasHeight;
  }

  public List<InkStroke> getStrokes() {
    return strokes;
  }

  public boolean isEmpty() {
    return strokes.isEmpty();
  }
}
