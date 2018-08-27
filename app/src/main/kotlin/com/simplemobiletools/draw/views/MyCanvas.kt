package com.simplemobiletools.draw.views

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.draw.R
import com.simplemobiletools.draw.interfaces.CanvasListener
import com.simplemobiletools.draw.models.MyParcelable
import com.simplemobiletools.draw.models.MyPath
import com.simplemobiletools.draw.models.PaintOptions
import java.util.*
import java.util.concurrent.ExecutionException

class MyCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val MIN_ERASER_WIDTH = 20f

    var mPaths = LinkedHashMap<MyPath, PaintOptions>()
    var mBackgroundBitmap: Bitmap? = null
    var mListener: CanvasListener? = null

    private var mLastPaths = LinkedHashMap<MyPath, PaintOptions>()
    private var mLastBackgroundBitmap: Bitmap? = null
    private var mUndonePaths = LinkedHashMap<MyPath, PaintOptions>()

    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mCurrBrushSize = 0f
    private var mIsSaving = false
    private var mAllowZooming = true
    private var mIsEraserOn = false
    private var mWasMultitouch = false
    private var mBackgroundColor = 0
    private var mCenter: PointF? = null

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1f

    init {
        mPaint.apply {
            color = mPaintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = mPaintOptions.strokeWidth
            isAntiAlias = true
        }

        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        pathsUpdated()
    }

    fun undo() {
        if (mPaths.isEmpty() && mLastPaths.isNotEmpty()) {
            mPaths = mLastPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
            mBackgroundBitmap = mLastBackgroundBitmap
            mLastPaths.clear()
            pathsUpdated()
            invalidate()
            return
        }

        if (mPaths.isEmpty()) {
            return
        }

        val lastPath = mPaths.values.lastOrNull()
        val lastKey = mPaths.keys.lastOrNull()

        mPaths.remove(lastKey)
        if (lastPath != null && lastKey != null) {
            mUndonePaths[lastKey] = lastPath
            mListener?.toggleRedoVisibility(true)
        }
        pathsUpdated()
        invalidate()
    }

    fun redo() {
        if (mUndonePaths.keys.isEmpty()) {
            mListener?.toggleRedoVisibility(false)
            return
        }

        val lastKey = mUndonePaths.keys.last()
        addPath(lastKey, mUndonePaths.values.last())
        mUndonePaths.remove(lastKey)
        if (mUndonePaths.isEmpty()) {
            mListener?.toggleRedoVisibility(false)
        }
        invalidate()
    }

    fun toggleEraser(isEraserOn: Boolean) {
        mIsEraserOn = isEraserOn
        mPaintOptions.isEraser = isEraserOn
        invalidate()
    }

    fun setColor(newColor: Int) {
        mPaintOptions.color = newColor
    }

    fun updateBackgroundColor(newColor: Int) {
        mBackgroundColor = newColor
        setBackgroundColor(newColor)
        mBackgroundBitmap = null
    }

    fun setBrushSize(newBrushSize: Float) {
        mCurrBrushSize = newBrushSize
        mPaintOptions.strokeWidth = resources.getDimension(R.dimen.full_brush_size) * (newBrushSize / mScaleFactor / 100f)
    }

    fun setAllowZooming(allowZooming: Boolean) {
        mAllowZooming = allowZooming
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        mIsSaving = true
        draw(canvas)
        mIsSaving = false
        return bitmap
    }

    fun drawBitmap(activity: Activity, path: Any) {
        Thread {
            val size = Point()
            activity.windowManager.defaultDisplay.getSize(size)
            val options = RequestOptions()
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .disallowHardwareConfig()
                    .fitCenter()

            try {
                val builder = Glide.with(context)
                        .asBitmap()
                        .load(path)
                        .apply(options)
                        .into(size.x, size.y)

                mBackgroundBitmap = builder.get()
                activity.runOnUiThread {
                    invalidate()
                }
            } catch (e: ExecutionException) {
                val errorMsg = String.format(activity.getString(R.string.failed_to_load_image), path)
                activity.toast(errorMsg)
            }
        }.start()
    }

    fun addPath(path: MyPath, options: PaintOptions) {
        mPaths[path] = options
        pathsUpdated()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        if (mCenter == null) {
            mCenter = PointF(width / 2f, height / 2f)
        }

        canvas.scale(mScaleFactor, mScaleFactor, mCenter!!.x, mCenter!!.y)

        if (mBackgroundBitmap != null) {
            val left = (width - mBackgroundBitmap!!.width) / 2
            val top = (height - mBackgroundBitmap!!.height) / 2
            canvas.drawBitmap(mBackgroundBitmap, left.toFloat(), top.toFloat(), null)
        }

        for ((key, value) in mPaths) {
            changePaint(value)
            canvas.drawPath(key, mPaint)
        }

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)
        canvas.restore()
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = if (paintOptions.isEraser) mBackgroundColor else paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
        if (paintOptions.isEraser && mPaint.strokeWidth < MIN_ERASER_WIDTH) {
            mPaint.strokeWidth = MIN_ERASER_WIDTH
        }
    }

    fun clearCanvas() {
        mLastPaths = mPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
        mLastBackgroundBitmap = mBackgroundBitmap
        mBackgroundBitmap = null
        mPath.reset()
        mPaths.clear()
        pathsUpdated()
        invalidate()
    }

    private fun actionDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        if (!mWasMultitouch) {
            mPath.lineTo(mCurX, mCurY)

            // draw a dot on click
            if (mStartX == mCurX && mStartY == mCurY) {
                mPath.lineTo(mCurX, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY)
            }
        }

        mPaths[mPath] = mPaintOptions
        pathsUpdated()
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth, mPaintOptions.isEraser)
    }

    private fun pathsUpdated() {
        mListener?.toggleUndoVisibility(mPaths.isNotEmpty() || mLastPaths.isNotEmpty())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mAllowZooming) {
            mScaleDetector!!.onTouchEvent(event)
        }

        var x = event.x
        var y = event.y

        if (mScaleFactor != 1f) {
            val fullWidth = width / mScaleFactor
            var curTouchX = fullWidth * x / width
            curTouchX -= (fullWidth / 2) * (1 - mScaleFactor)
            x = curTouchX

            val fullHeight = height / mScaleFactor
            var curTouchY = fullHeight * y / height
            curTouchY -= (fullHeight / 2) * (1 - mScaleFactor)
            y = curTouchY
        }

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mWasMultitouch = false
                mStartX = x
                mStartY = y
                actionDown(x, y)
                mUndonePaths.clear()
                mListener?.toggleRedoVisibility(false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!mAllowZooming || (!mScaleDetector!!.isInProgress && event.pointerCount == 1 && !mWasMultitouch)) {
                    actionMove(x, y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> actionUp()
            MotionEvent.ACTION_POINTER_DOWN -> mWasMultitouch = true
        }

        invalidate()
        return true
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = MyParcelable(superState)
        savedState.paths = mPaths
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is MyParcelable) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        mPaths = state.paths
        pathsUpdated()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f))
            setBrushSize(mCurrBrushSize)
            invalidate()
            return true
        }
    }
}
