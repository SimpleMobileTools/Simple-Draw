package draw.simplemobiletools.com;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MyCanvas extends View {
    private static final float THRESHOLD = 5;
    private Paint paint;
    private Path path;
    private float startX;
    private float startY;

    public MyCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);

        path = new Path();
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    private void actionDown(float x, float y) {
        path.moveTo(x, y);
        startX = x;
        startY = y;
    }

    private void actionMove(float x, float y) {
        final float dx = Math.abs(x - startX);
        final float dy = Math.abs(y - startY);
        if (dx >= THRESHOLD || dy >= THRESHOLD) {
            path.quadTo(startX, startY, (x + startX) / 2, (y + startY) / 2);
            startX = x;
            startY = y;
        }
    }

    private void actionUp() {
        path.lineTo(startX, startY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDown(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                actionMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                actionUp();
                invalidate();
                break;
            default:
                break;
        }

        return true;
    }
}
