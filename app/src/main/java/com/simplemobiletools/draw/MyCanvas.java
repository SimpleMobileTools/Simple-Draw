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
    private Paint mPaint;
    private Path mPath;
    private Map<Path, Integer> mPaths;
    private PathsChangedListener mListener;

    private int mColor;
    private float mCurX;
    private float mCurY;
    private float mStartX;
    private float mStartY;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5f);
        mPaint.setAntiAlias(true);

        mPaths = new LinkedHashMap<>();
        mPaths.put(mPath, mPaint.getColor());
        pathsUpdated();
    }

    public void setListener(PathsChangedListener listener) {
        this.mListener = listener;
    }

    public void undo() {
        if (mPaths.size() <= 0)
            return;

        Path lastKey = null;
        for (Path key : mPaths.keySet()) {
            lastKey = key;
        }

        mPaths.remove(lastKey);
        pathsUpdated();
        invalidate();
    }

    public void setColor(int newColor) {
        mColor = newColor;
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

        for (Map.Entry<Path, Integer> entry : mPaths.entrySet()) {
            mPaint.setColor(entry.getValue());
            canvas.drawPath(entry.getKey(), mPaint);
        }

        mPaint.setColor(mColor);
        canvas.drawPath(mPath, mPaint);
    }

    private void actionDown(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mCurX = x;
        mCurY = y;
    }

    private void actionMove(float x, float y) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2);
        mCurX = x;
        mCurY = y;
    }

    private void actionUp() {
        mPath.lineTo(mCurX, mCurY);

        // draw a dot on click
        if (mStartX == mCurX && mStartY == mCurY) {
            mPath.lineTo(mCurX, mCurY + 2);
            mPath.lineTo(mCurX + 1, mCurY + 2);
            mPath.lineTo(mCurX + 1, mCurY);
        }

        mPaths.put(mPath, mPaint.getColor());
        pathsUpdated();
        mPath = new Path();
    }

    private void pathsUpdated() {
        if (mListener != null && mPaths != null) {
            mListener.pathsChanged(mPaths.size());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = x;
                mStartY = y;
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

    public interface PathsChangedListener {
        void pathsChanged(int cnt);
    }
}
