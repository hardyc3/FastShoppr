package com.fastshoppr.harness;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.mlkit.vision.digitalink.recognition.Ink;

import java.util.ArrayList;
import java.util.List;

public final class InkCanvasView extends View {

    private static final class PointData {
        final float x;
        final float y;
        final long timestamp;

        PointData(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }
    }

    private static final class StrokeData {
        final List<PointData> points = new ArrayList<>();
    }

    private final List<StrokeData> strokes = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private StrokeData currentStroke;

    public InkCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFF111111);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setFocusable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                currentStroke = new StrokeData();
                currentStroke.points.add(
                        new PointData(event.getX(), event.getY(), event.getEventTime()));
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentStroke != null) {
                    currentStroke.points.add(
                            new PointData(event.getX(), event.getY(), event.getEventTime()));
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentStroke != null) {
                    currentStroke.points.add(
                            new PointData(event.getX(), event.getY(), event.getEventTime()));
                    strokes.add(currentStroke);
                    currentStroke = null;
                    invalidate();
                    performClick();
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (StrokeData stroke : strokes) {
            drawStroke(canvas, stroke);
        }
        if (currentStroke != null) {
            drawStroke(canvas, currentStroke);
        }
    }

    private void drawStroke(Canvas canvas, StrokeData stroke) {
        if (stroke.points.isEmpty()) {
            return;
        }
        path.reset();
        PointData first = stroke.points.get(0);
        path.moveTo(first.x, first.y);
        for (int i = 1; i < stroke.points.size(); i++) {
            PointData point = stroke.points.get(i);
            path.lineTo(point.x, point.y);
        }
        canvas.drawPath(path, paint);
    }

    public boolean isEmpty() {
        return strokes.isEmpty() && currentStroke == null;
    }

    public void clear() {
        strokes.clear();
        currentStroke = null;
        invalidate();
    }

    public Ink buildInk() {
        Ink.Builder inkBuilder = Ink.builder();
        for (StrokeData stroke : strokes) {
            Ink.Stroke.Builder strokeBuilder = Ink.Stroke.builder();
            for (PointData point : stroke.points) {
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, point.timestamp));
            }
            inkBuilder.addStroke(strokeBuilder.build());
        }
        return inkBuilder.build();
    }
}
