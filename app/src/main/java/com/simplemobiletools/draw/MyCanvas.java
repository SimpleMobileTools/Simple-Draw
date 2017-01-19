package com.simplemobiletools.draw;

import android.content.Context;
import android.content.res.Resources;
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
    private Map<MyPath, PaintOptions> mPaths;
    private PathsChangedListener mListener;

    private PaintOptions mPaintOptions;
    private float mCurX;
    private float mCurY;
    private float mStartX;
    private float mStartY;
    private boolean mIsSaving = false;
    private boolean mIsStrokeWidthBarEnabled = false;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPath = new MyPath();
        mPaint = new Paint();
        mPaintOptions = new PaintOptions();
        mPaint.setColor(mPaintOptions.color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mPaintOptions.strokeWidth);
        mPaint.setAntiAlias(true);

        mPaths = new LinkedHashMap<>();
        mPaths.put(mPath, mPaintOptions);
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
        mPaintOptions.color = newColor;
        if (mIsStrokeWidthBarEnabled) {
            invalidate();
        }
    }

    public void setStrokeWidth(float newStrokeWidth) {
        mPaintOptions.strokeWidth = newStrokeWidth;
        if (mIsStrokeWidthBarEnabled) {
            invalidate();
        }
    }

    public void setIsStrokeWidthBarEnabled(boolean isStrokeWidthBarEnabled) {
        mIsStrokeWidthBarEnabled = isStrokeWidthBarEnabled;
        invalidate();
    }

    public Bitmap getBitmap() {
        final Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        mIsSaving = true;
        draw(canvas);
        mIsSaving = false;
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Map.Entry<MyPath, PaintOptions> entry : mPaths.entrySet()) {
            changePaint(entry.getValue());
            canvas.drawPath(entry.getKey(), mPaint);
        }

        changePaint(mPaintOptions);
        canvas.drawPath(mPath, mPaint);

        if (mIsStrokeWidthBarEnabled && !mIsSaving) {
            drawPreviewCircle(canvas);
        }
    }

    private void drawPreviewCircle(Canvas canvas) {
        Resources res = getResources();
        mPaint.setStyle(Paint.Style.FILL);

        float y = getHeight() - res.getDimension(R.dimen.preview_dot_offset_y);
        canvas.drawCircle(getWidth() / 2, y, mPaintOptions.strokeWidth / 2, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Utils.shouldUseWhite(mPaintOptions.color) ? Color.WHITE : Color.BLACK);
        mPaint.setStrokeWidth(res.getDimension(R.dimen.preview_dot_stroke_size));

        y = getHeight() - res.getDimension(R.dimen.preview_dot_offset_y);
        float radius = (mPaintOptions.strokeWidth + res.getDimension(R.dimen.preview_dot_stroke_size)) / 2;
        canvas.drawCircle(getWidth() / 2, y, radius, mPaint);
        changePaint(mPaintOptions);
    }

    private void changePaint(PaintOptions paintOptions) {
        mPaint.setColor(paintOptions.color);
        mPaint.setStrokeWidth(paintOptions.strokeWidth);
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

        mPaths.put(mPath, mPaintOptions);
        pathsUpdated();
        mPath = new MyPath();
        mPaintOptions = new PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth);
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
        Map<MyPath, PaintOptions> mPaths;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mPaths.size());
            for (Map.Entry<MyPath, PaintOptions> entry : mPaths.entrySet()) {
                out.writeSerializable(entry.getKey());
                PaintOptions paintOptions = entry.getValue();
                out.writeInt(paintOptions.color);
                out.writeFloat(paintOptions.strokeWidth);
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
                PaintOptions paintOptions = new PaintOptions(in.readInt(), in.readFloat());
                mPaths.put(key, paintOptions);
            }
        }
    }
}
