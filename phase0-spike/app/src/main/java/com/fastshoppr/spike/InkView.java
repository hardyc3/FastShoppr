package com.fastshoppr.spike;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.mlkit.vision.digitalink.recognition.Ink;

import java.util.ArrayList;
import java.util.List;

public class InkView extends View {
    public static class StrokeData {
        public final List<float[]> points = new ArrayList<>(); // x, y, tMillis
    }

    private final List<StrokeData> strokes = new ArrayList<>();
    private StrokeData current;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public InkView(Context context) { super(context); init(); }
    public InkView(Context context, AttributeSet attrs) { super(context); init(); }

    private void init() {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(6f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        for (StrokeData s : strokes) drawStroke(canvas, s);
        if (current != null) drawStroke(canvas, current);
    }

    private void drawStroke(Canvas canvas, StrokeData s) {
        if (s.points.isEmpty()) return;
        Path p = new Path();
        float[] first = s.points.get(0);
        p.moveTo(first[0], first[1]);
        for (int i = 1; i < s.points.size(); i++) {
            float[] pt = s.points.get(i);
            p.lineTo(pt[0], pt[1]);
        }
        canvas.drawPath(p, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        long t = event.getEventTime();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                current = new StrokeData();
                current.points.add(new float[]{x, y, t});
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current != null) {
                    for (int i = 0; i < event.getHistorySize(); i++) {
                        current.points.add(new float[]{event.getHistoricalX(i), event.getHistoricalY(i), event.getHistoricalEventTime(i)});
                    }
                    current.points.add(new float[]{x, y, t});
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (current != null) {
                    current.points.add(new float[]{x, y, t});
                    strokes.add(current);
                    current = null;
                    invalidate();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    public void clear() {
        strokes.clear();
        current = null;
        invalidate();
    }

    public Ink buildInk() {
        Ink.Builder builder = Ink.builder();
        for (StrokeData s : strokes) {
            if (s.points.isEmpty()) continue;
            Ink.Stroke.Builder stroke = Ink.Stroke.builder();
            for (float[] pt : s.points) {
                stroke.addPoint(Ink.Point.create(pt[0], pt[1], (long) pt[2]));
            }
            builder.addStroke(stroke.build());
        }
        return builder.build();
    }

    public boolean isEmpty() { return strokes.isEmpty(); }
}
