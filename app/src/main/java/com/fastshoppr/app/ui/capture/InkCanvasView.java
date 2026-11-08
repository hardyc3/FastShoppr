/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class InkCanvasView extends View {

  private final Paint paint;
  private final Path path;
  private boolean ready;

  private boolean currentStrokeStarted = false;

  public InkCanvasView(Context context, AttributeSet attrs) {
    super(context, attrs);
    paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(4f);
    paint.setAntiAlias(true);
    path = new Path();
    ready = false;
  }

  public void onTouchDown(float x, float y) {
    path.moveTo(x, y);
    currentStrokeStarted = true;
  }

  public void onTouchMove(float x, float y) {
    path.lineTo(x, y);
  }

  public void onTouchUp() {
    currentStrokeStarted = false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        onTouchDown(x, y);
        break;
      case MotionEvent.ACTION_MOVE:
        onTouchMove(x, y);
        break;
      case MotionEvent.ACTION_UP:
        onTouchUp();
        break;
      default:
        break;
    }
    return true;
  }

  public boolean isDrawing() {
    return currentStrokeStarted;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    ready = w > 0 && h > 0;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawPath(path, paint);
  }

  public void resetStrokes() {
    path.reset();
    currentStrokeStarted = false;
    invalidate();
  }
}
