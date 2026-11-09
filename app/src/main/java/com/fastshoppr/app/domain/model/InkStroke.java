/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One handwriting stroke, kept in natural capture order. */
public final class InkStroke {

  private final List<InkPoint> points;

  public InkStroke(List<InkPoint> points) {
    if (points == null || points.isEmpty()) {
      throw new IllegalArgumentException("stroke must contain at least one point");
    }
    this.points = Collections.unmodifiableList(new ArrayList<>(points));
  }

  public List<InkPoint> getPoints() {
    return points;
  }
}
