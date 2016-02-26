package com.simplemobiletools.draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyCanvas extends View {
    private Paint paint;
    private Path path;
    private Map<Path, Integer> paths;
    private int color;
    private float curX;
    private float curY;
    private float startX;
    private float startY;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);

        path = new Path();
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5f);
        paint.setAntiAlias(true);

        paths = new LinkedHashMap<>();
        paths.put(path, paint.getColor());
    }

    public void undo() {
        if (paths.size() <= 0)
            return;

        Path lastKey = null;
        for (Path key : paths.keySet()) {
            lastKey = key;
        }

        paths.remove(lastKey);
        invalidate();
    }

    public void setColor(int newColor) {
        color = newColor;
    }

    public Bitmap getBitmap() {
        final Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        draw(canvas);
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Map.Entry<Path, Integer> entry : paths.entrySet()) {
            paint.setColor(entry.getValue());
            canvas.drawPath(entry.getKey(), paint);
        }

        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    private void actionDown(float x, float y) {
        path.reset();
        path.moveTo(x, y);
        curX = x;
        curY = y;
    }

    private void actionMove(float x, float y) {
        path.quadTo(curX, curY, (x + curX) / 2, (y + curY) / 2);
        curX = x;
        curY = y;
    }

    private void actionUp() {
        path.lineTo(curX, curY);

        // drawing dots on click
        if (startX == curX && startY == curY) {
            path.lineTo(curX, curY + 2);
            path.lineTo(curX + 1, curY + 2);
            path.lineTo(curX + 1, curY);
        }

        paths.put(path, paint.getColor());
        path = new Path();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                actionDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                actionMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                actionUp();
                break;
            default:
                break;
        }

        invalidate();
        return true;
    }
}
