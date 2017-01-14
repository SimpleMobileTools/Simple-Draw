package com.simplemobiletools.draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyCanvas extends View {
    private Paint mPaint;
    private MyPath mPath;
    private Map<MyPath, Integer> mPaths;
    private PathsChangedListener mListener;

    private int mColor;
    private float mCurX;
    private float mCurY;
    private float mStartX;
    private float mStartY;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPath = new MyPath();
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

        MyPath lastKey = null;
        for (MyPath key : mPaths.keySet()) {
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

        for (Map.Entry<MyPath, Integer> entry : mPaths.entrySet()) {
            mPaint.setColor(entry.getValue());
            canvas.drawPath(entry.getKey(), mPaint);
        }

        mPaint.setColor(mColor);
        canvas.drawPath(mPath, mPaint);
    }

    public void clearCanvas() {
        mPath.reset();
        mPaths.clear();
        pathsUpdated();
        invalidate();
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
        mPath = new MyPath();
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

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);

        savedState.mPaths = mPaths;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mPaths = savedState.mPaths;
        pathsUpdated(); // This doesn't seem to be necessary
    }

    static class SavedState extends BaseSavedState {
        Map<MyPath, Integer> mPaths;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mPaths.size());
            for (Map.Entry<MyPath, Integer> entry : mPaths.entrySet()) {
                out.writeSerializable(entry.getKey());
                out.writeInt(entry.getValue());
            }
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        private SavedState(Parcel in) {
            super(in);
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                MyPath key = (MyPath) in.readSerializable();
                int value = in.readInt();
                mPaths.put(key, value);
            }
        }
    }
}
