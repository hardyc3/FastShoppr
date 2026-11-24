/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.fastshoppr.app.domain.model.InkData;
import com.fastshoppr.app.domain.model.InkPoint;
import com.fastshoppr.app.domain.model.InkStroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Writing canvas that records strokes as {@link InkPoint} values with timestamps, in natural
 * capture order. Finger and stylus input are both accepted.
 */
public final class InkCanvasView extends View {

  private static final float MIN_POINT_DISTANCE_DP = 2.0f;
  private static final float STROKE_WIDTH_DP = 4.0f;

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final List<List<InkPoint>> strokes = new ArrayList<>();

  @Nullable private List<InkPoint> currentStroke;
  private boolean currentStrokeStarted;
  private float minPointDistancePx;

  public InkCanvasView(Context context, AttributeSet attrs) {
    super(context, attrs);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeJoin(Paint.Join.ROUND);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeWidth(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            STROKE_WIDTH_DP,
            context.getResources().getDisplayMetrics()));
    minPointDistancePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            MIN_POINT_DISTANCE_DP,
            context.getResources().getDisplayMetrics());
    applyThemeColor(context);
  }

  private void applyThemeColor(Context context) {
    TypedValue value = new TypedValue();
    if (context
            .getTheme()
            .resolveAttribute(com.google.android.material.R.attr.colorOnSurface, value, true)
        && value.resourceId != 0) {
      paint.setColor(context.getColor(value.resourceId));
    } else if (context
            .getTheme()
            .resolveAttribute(android.R.attr.colorControlHighlight, value, true)
        && value.resourceId != 0) {
      paint.setColor(context.getColor(value.resourceId));
    } else {
      paint.setColor(0xFF000000);
    }
  }

  public void onTouchDown(float x, float y) {
    currentStroke = new ArrayList<>();
    currentStroke.add(new InkPoint(x, y, System.currentTimeMillis()));
    currentStrokeStarted = true;
    invalidate();
  }

  public void onTouchMove(float x, float y) {
    if (!currentStrokeStarted || currentStroke == null || currentStroke.isEmpty()) {
      return;
    }
    InkPoint last = currentStroke.get(currentStroke.size() - 1);
    float dx = x - last.getX();
    float dy = y - last.getY();
    if (dx * dx + dy * dy < minPointDistancePx * minPointDistancePx) {
      return;
    }
    currentStroke.add(new InkPoint(x, y, System.currentTimeMillis()));
    invalidate();
  }

  public void onTouchUp() {
    if (currentStrokeStarted && currentStroke != null && !currentStroke.isEmpty()) {
      strokes.add(currentStroke);
    }
    currentStroke = null;
    currentStrokeStarted = false;
    invalidate();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        if (currentStrokeStarted) {
          onTouchUp();
        }
        onTouchDown(x, y);
        return true;
      case MotionEvent.ACTION_MOVE:
        onTouchMove(x, y);
        return true;
      case MotionEvent.ACTION_UP:
        onTouchUp();
        performClick();
        return true;
      case MotionEvent.ACTION_CANCEL:
        onTouchUp();
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }

  public boolean isDrawing() {
    return currentStrokeStarted;
  }

  public boolean hasStrokes() {
    return !strokes.isEmpty();
  }

  public void undoLastStroke() {
    if (!strokes.isEmpty()) {
      strokes.remove(strokes.size() - 1);
    }
    invalidate();
  }

  public void resetStrokes() {
    strokes.clear();
    currentStroke = null;
    currentStrokeStarted = false;
    invalidate();
  }

  /** Exports the captured writing as {@link InkData} for recognition. */
  public InkData getInkData() {
    List<InkStroke> result = new ArrayList<>(strokes.size());
    for (List<InkPoint> stroke : strokes) {
      result.add(new InkStroke(stroke));
    }
    return new InkData(getWidth(), getHeight(), result);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    for (List<InkPoint> stroke : strokes) {
      drawStroke(canvas, stroke);
    }
    if (currentStrokeStarted && currentStroke != null && !currentStroke.isEmpty()) {
      drawStroke(canvas, currentStroke);
    }
  }

  private void drawStroke(Canvas canvas, List<InkPoint> stroke) {
    if (stroke.isEmpty()) {
      return;
    }
    Path path = new Path();
    InkPoint first = stroke.get(0);
    path.moveTo(first.getX(), first.getY());
    for (int index = 1; index < stroke.size(); index++) {
      InkPoint point = stroke.get(index);
      path.lineTo(point.getX(), point.getY());
    }
    canvas.drawPath(path, paint);
  }
}
