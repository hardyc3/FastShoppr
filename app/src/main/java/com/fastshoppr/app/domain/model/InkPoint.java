/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** One captured handwriting point with its timestamp. */
public final class InkPoint {

  private final float x;
  private final float y;
  private final long timestampMillis;

  public InkPoint(float x, float y, long timestampMillis) {
    this.x = x;
    this.y = y;
    this.timestampMillis = timestampMillis;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public long getTimestampMillis() {
    return timestampMillis;
  }
}
