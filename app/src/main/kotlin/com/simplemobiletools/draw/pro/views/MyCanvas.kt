package com.simplemobiletools.draw.pro.views

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.draw.pro.R
import com.simplemobiletools.draw.pro.extensions.contains
import com.simplemobiletools.draw.pro.extensions.floodFill
import com.simplemobiletools.draw.pro.interfaces.CanvasListener
import com.simplemobiletools.draw.pro.models.CanvasOp
import com.simplemobiletools.draw.pro.models.MyParcelable
import com.simplemobiletools.draw.pro.models.MyPath
import com.simplemobiletools.draw.pro.models.PaintOptions
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MyCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val MIN_ERASER_WIDTH = 20f
    private val MAX_HISTORY_COUNT = 1000
    private val BITMAP_MAX_HISTORY_COUNT = 60
    private val DEFAULT_FLOOD_FILL_TOLERANCE = 190

    private val mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var mOperations = ArrayList<CanvasOp>()
    var mBackgroundBitmap: Bitmap? = null
    var mListener: CanvasListener? = null

    private var mUndoneOperations = ArrayList<CanvasOp>()
    private var mLastOperations = ArrayList<CanvasOp>()
    private var mLastBackgroundBitmap: Bitmap? = null

    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mActivePointerId = INVALID_POINTER_ID

    private var mCurrBrushSize = 0f
    private var mAllowMovingZooming = true
    private var mIsEraserOn = false
    private var mIsBucketFillOn = false
    private var mWasMultitouch = false
    private var mIgnoreTouches = false
    private var mWasScalingInGesture = false
    private var mWasMovingCanvasInGesture = false
    private var mBackgroundColor = 0
    private var mCenter: PointF? = null

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1f

    private var mLastMotionEvent: MotionEvent? = null
    private var mTouchSloppedBeforeMultitouch: Boolean = false

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
        updateUndoVisibility()
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = MyParcelable(superState!!)
        savedState.operations = mOperations
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is MyParcelable) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        mOperations = state.operations
        updateUndoVisibility()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mAllowMovingZooming) {
            mScaleDetector!!.onTouchEvent(event)
        }

        val action = event.actionMasked
        if (mIgnoreTouches && action == MotionEvent.ACTION_UP) {
            mIgnoreTouches = false
            mWasScalingInGesture = false
            mWasMovingCanvasInGesture = false
            return true
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            mActivePointerId = event.getPointerId(0)
        }

        val pointerIndex = event.findPointerIndex(mActivePointerId)
        val x: Float
        val y: Float

        try {
            x = event.getX(pointerIndex)
            y = event.getY(pointerIndex)
        } catch (e: Exception) {
            return true
        }

        val scaledWidth = width / mScaleFactor
        val touchPercentageX = x / width
        val compensationX = (scaledWidth / 2) * (1 - mScaleFactor)
        val newValueX = scaledWidth * touchPercentageX - compensationX - (mPosX / mScaleFactor)

        val scaledHeight = height / mScaleFactor
        val touchPercentageY = y / height
        val compensationY = (scaledHeight / 2) * (1 - mScaleFactor)
        val newValueY = scaledHeight * touchPercentageY - compensationY - (mPosY / mScaleFactor)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mWasScalingInGesture = false
                mWasMovingCanvasInGesture = false
                mWasMultitouch = false
                mStartX = x
                mStartY = y
                mLastTouchX = x
                mLastTouchY = y
                actionDown(newValueX, newValueY)
                mUndoneOperations.clear()
                updateRedoVisibility(false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mTouchSloppedBeforeMultitouch) {
                    mPath.reset()
                    mTouchSloppedBeforeMultitouch = false
                }

                if (!mIsBucketFillOn && (!mAllowMovingZooming || (!mScaleDetector!!.isInProgress && event.pointerCount == 1 && !mWasMultitouch))) {
                    actionMove(newValueX, newValueY)
                }

                if (mAllowMovingZooming && mWasMultitouch) {
                    mPosX += x - mLastTouchX
                    mPosY += y - mLastTouchY
                    mWasMovingCanvasInGesture = true
                    invalidate()
                }

                mLastTouchX = x
                mLastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
                actionUp(false)
                mWasScalingInGesture = false
                mWasMovingCanvasInGesture = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (mAllowMovingZooming) {
                    mWasMultitouch = true
                    mTouchSloppedBeforeMultitouch = mLastMotionEvent.isTouchSlop(pointerIndex, mStartX, mStartY)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (mAllowMovingZooming) {
                    mIgnoreTouches = true
                    actionUp(!mWasScalingInGesture && !mWasMovingCanvasInGesture)
                }
            }
        }

        mLastMotionEvent = MotionEvent.obtain(event)
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        if (mCenter == null) {
            mCenter = PointF(width / 2f, height / 2f)
        }

        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor, mCenter!!.x, mCenter!!.y)

        if (mBackgroundBitmap != null) {
            val bitmap = mBackgroundBitmap!!
            val left = (width - bitmap.width) / 2f
            val top = (height - bitmap.height) / 2f
            canvas.drawBitmap(bitmap, left, top, null)
        }

        if (mOperations.isNotEmpty()) {
            val bitmapOps = mOperations.filterIsInstance<CanvasOp.BitmapOp>()
            val bitmapOp = bitmapOps.lastOrNull()
            if (bitmapOp != null) {
                canvas.drawBitmap(bitmapOp.bitmap, 0f, 0f, null)
            }

            // only perform path ops after last bitmap op as any previous path operations are already visible due to the bitmap op
            val startIndex = if (bitmapOp != null) mOperations.indexOf(bitmapOp) else 0
            val endIndex = mOperations.lastIndex
            val pathOps = mOperations.slice(startIndex..endIndex).filterIsInstance<CanvasOp.PathOp>()
            for (pathOp in pathOps) {
                changePaint(pathOp.paintOptions)
                canvas.drawPath(pathOp.path, mPaint)
            }
        }

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)
        canvas.restore()
    }

    fun undo() {
        if (mOperations.isEmpty() && mLastOperations.isNotEmpty()) {
            mOperations = mLastOperations.clone() as ArrayList<CanvasOp>
            mBackgroundBitmap = mLastBackgroundBitmap
            mLastOperations.clear()
            updateUndoVisibility()
            invalidate()
            return
        }

        if (mOperations.isNotEmpty()) {
            val lastOp = mOperations.removeLast()
            mUndoneOperations.add(lastOp)
            invalidate()
        }
        updateUndoRedoVisibility()
    }

    fun redo() {
        if (mUndoneOperations.isNotEmpty()) {
            val undoneOperation = mUndoneOperations.removeLast()
            addOperation(undoneOperation)
            invalidate()
        }
        updateUndoRedoVisibility()
    }

    fun toggleEraser(isEraserOn: Boolean) {
        mIsEraserOn = isEraserOn
        mPaintOptions.isEraser = isEraserOn
        invalidate()
    }

    fun toggleBucketFill(isBucketFillOn: Boolean) {
        mIsBucketFillOn = isBucketFillOn
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
        mPaintOptions.strokeWidth = resources.getDimension(R.dimen.full_brush_size) * (mCurrBrushSize / mScaleFactor / 100f)
    }

    fun setAllowZooming(allowZooming: Boolean) {
        mAllowMovingZooming = allowZooming
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }

    fun drawBitmap(activity: Activity, path: Any) {
        ensureBackgroundThread {
            val size = Point()
            activity.windowManager.defaultDisplay.getSize(size)
            val options = RequestOptions().format(DecodeFormat.PREFER_ARGB_8888).disallowHardwareConfig().fitCenter()

            try {
                val builder = Glide.with(context).asBitmap().load(path).apply(options).submit(size.x, size.y)

                mBackgroundBitmap = builder.get()
                activity.runOnUiThread {
                    invalidate()
                }
            } catch (e: ExecutionException) {
                val errorMsg = String.format(activity.getString(R.string.failed_to_load_image), path)
                activity.toast(errorMsg)
            }
        }
    }

    fun addPath(path: MyPath, options: PaintOptions) {
        val pathOp = CanvasOp.PathOp(path, options)
        mOperations.add(pathOp)
        updateUndoVisibility()
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = if (paintOptions.isEraser) mBackgroundColor else paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
        if (paintOptions.isEraser && mPaint.strokeWidth < MIN_ERASER_WIDTH) {
            mPaint.strokeWidth = MIN_ERASER_WIDTH
        }
    }

    fun clearCanvas() {
        mLastOperations = mOperations.clone() as ArrayList<CanvasOp>
        mLastBackgroundBitmap = mBackgroundBitmap
        mBackgroundBitmap = null
        mPath.reset()
        mOperations.clear()
        updateUndoVisibility()
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

    private fun actionUp(forceLineDraw: Boolean) {
        if (mIsBucketFillOn) {
            bucketFill()
        } else if (!mWasMultitouch || forceLineDraw) {
            drawADot()
        }

        updateUndoVisibility()
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth, mPaintOptions.isEraser)
    }

    private fun updateUndoRedoVisibility() {
        updateUndoVisibility()
        updateRedoVisibility()
    }

    private fun updateUndoVisibility() {
        mListener?.toggleUndoVisibility(mOperations.isNotEmpty() || mLastOperations.isNotEmpty())
    }

    private fun updateRedoVisibility(visible: Boolean = mUndoneOperations.isNotEmpty()) {
        mListener?.toggleRedoVisibility(visible)
    }

    private fun bucketFill() {
        val touchedX = mCurX.toInt()
        val touchedY = mCurY.toInt()
        if (contains(touchedX, touchedY)) {
            val bitmap = getBitmap()
            val color = mPaintOptions.color

            ensureBackgroundThread {
                val img = bitmap.floodFill(color = color, x = touchedX, y = touchedY, tolerance = DEFAULT_FLOOD_FILL_TOLERANCE)
                addOperation(CanvasOp.BitmapOp(img))
                post { invalidate() }
            }
        }
    }

    private fun drawADot() {
        mPath.lineTo(mCurX, mCurY)

        // draw a dot on click
        if (mStartX == mCurX && mStartY == mCurY) {
            mPath.lineTo(mCurX, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY)
        }
        addOperation(CanvasOp.PathOp(mPath, mPaintOptions))
    }

    private fun addOperation(operation: CanvasOp) {
        mOperations.add(operation)

        // maybe free up some memory
        while (mOperations.size > MAX_HISTORY_COUNT) {
            val item = mOperations.removeFirst()
            if (item is CanvasOp.BitmapOp) {
                item.bitmap.recycle()
            }
        }

        val ops = mOperations.filterIsInstance<CanvasOp.BitmapOp>()
        if (ops.size > BITMAP_MAX_HISTORY_COUNT) {
            val start = ops.lastIndex - BITMAP_MAX_HISTORY_COUNT
            val bitmapOp = ops.slice(start..ops.lastIndex).first()

            val startIndex = mOperations.indexOf(bitmapOp)
            mOperations = mOperations.slice(startIndex..mOperations.lastIndex) as ArrayList<CanvasOp>
        }
    }

    fun getPathsMap(): Map<MyPath, PaintOptions> {
        val pathOps = mOperations
            .filterIsInstance<CanvasOp.PathOp>()
            .map { it.path to it.paintOptions }
            .toTypedArray()
        return mapOf(*pathOps)
    }

    fun getDrawingHashCode() = mOperations.hashCode().toLong() + (mBackgroundBitmap?.hashCode()?.toLong() ?: 0L)

    private fun MotionEvent?.isTouchSlop(pointerIndex: Int, startX: Float, startY: Float): Boolean {
        return if (this == null || actionMasked != MotionEvent.ACTION_MOVE) {
            false
        } else {
            try {
                val moveX = abs(getX(pointerIndex) - startX)
                val moveY = abs(getY(pointerIndex) - startY)

                moveX <= mScaledTouchSlop && moveY <= mScaledTouchSlop
            } catch (e: Exception) {
                false
            }
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!mWasScalingInGesture) {
                mPath.reset()
            }

            mIgnoreTouches = true
            mWasScalingInGesture = true
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = max(0.1f, min(mScaleFactor, 10.0f))
            setBrushSize(mCurrBrushSize)
            invalidate()
            return true
        }
    }
}
