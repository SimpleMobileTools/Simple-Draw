package draw.simplemobiletools.com;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class MyCanvas extends View {
    private Paint paint;
    private Path path;
    private ArrayList<Path> paths;
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

        paths = new ArrayList<>();
        paths.add(path);
    }

    public void undo() {
        if (paths.size() <= 0)
            return;

        paths.remove(paths.size() - 1);
        invalidate();
    }

    public void setColor(int color) {
        paint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Path p : paths) {
            canvas.drawPath(p, paint);
        }
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

        paths.add(path);
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
