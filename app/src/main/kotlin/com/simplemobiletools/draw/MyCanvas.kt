package com.simplemobiletools.draw

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.draw.models.MyParcelable
import com.simplemobiletools.draw.models.MyPath
import com.simplemobiletools.draw.models.PaintOptions
import java.util.*
import java.util.concurrent.ExecutionException

class MyCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val MIN_ERASER_WIDTH = 20f

    var mPaths = LinkedHashMap<MyPath, PaintOptions>()
    var mBackgroundBitmap: Bitmap? = null
    var mListener: PathsChangedListener? = null

    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mIsSaving = false
    private var mIsStrokeWidthBarEnabled = false
    private var mIsEraserOn = false
    private var mBackgroundColor = 0

    init {
        mPaint.apply {
            color = mPaintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = mPaintOptions.strokeWidth
            isAntiAlias = true
        }

        pathsUpdated()
    }

    fun undo() {
        if (mPaths.isEmpty())
            return

        val lastKey = mPaths.keys.lastOrNull()

        mPaths.remove(lastKey)
        pathsUpdated()
        invalidate()
    }

    fun toggleEraser(isEraserOn: Boolean) {
        mIsEraserOn = isEraserOn
        mPaintOptions.isEraser = isEraserOn
        invalidate()
    }

    fun setColor(newColor: Int) {
        mPaintOptions.color = newColor
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun updateBackgroundColor(newColor: Int) {
        mBackgroundColor = newColor
        setBackgroundColor(newColor)
        mBackgroundBitmap = null
    }

    fun setStrokeWidth(newStrokeWidth: Float) {
        mPaintOptions.strokeWidth = newStrokeWidth
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun setIsStrokeWidthBarEnabled(isStrokeWidthBarEnabled: Boolean) {
        mIsStrokeWidthBarEnabled = isStrokeWidthBarEnabled
        invalidate()
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
        Thread({
            val size = Point()
            activity.windowManager.defaultDisplay.getSize(size)
            val options = RequestOptions()
                    .format(DecodeFormat.PREFER_ARGB_8888)
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
        }).start()
    }

    fun addPath(path: MyPath, options: PaintOptions) {
        mPaths.put(path, options)
        pathsUpdated()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

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

        if (mIsStrokeWidthBarEnabled && !mIsSaving) {
            drawPreviewCircle(canvas)
        }
    }

    private fun drawPreviewCircle(canvas: Canvas) {
        val res = resources
        mPaint.style = Paint.Style.FILL

        var y = height - res.getDimension(R.dimen.preview_dot_offset_y)
        canvas.drawCircle((width / 2).toFloat(), y, mPaintOptions.strokeWidth / 2, mPaint)
        mPaint.style = Paint.Style.STROKE
        mPaint.color = if (mPaintOptions.isEraser) mBackgroundColor.getContrastColor() else mPaintOptions.color.getContrastColor()
        mPaint.strokeWidth = res.getDimension(R.dimen.preview_dot_stroke_size)

        y = height - res.getDimension(R.dimen.preview_dot_offset_y)
        val radius = (mPaintOptions.strokeWidth + res.getDimension(R.dimen.preview_dot_stroke_size)) / 2
        canvas.drawCircle((width / 2).toFloat(), y, radius, mPaint)
        changePaint(mPaintOptions)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = if (paintOptions.isEraser) mBackgroundColor else paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
        if (paintOptions.isEraser && mPaint.strokeWidth < MIN_ERASER_WIDTH) {
            mPaint.strokeWidth = MIN_ERASER_WIDTH
        }
    }

    fun clearCanvas() {
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
        mPath.lineTo(mCurX, mCurY)

        // draw a dot on click
        if (mStartX == mCurX && mStartY == mCurY) {
            mPath.lineTo(mCurX, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY)
        }

        mPaths.put(mPath, mPaintOptions)
        pathsUpdated()
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth, mPaintOptions.isEraser)
    }

    private fun pathsUpdated() {
        mListener?.pathsChanged(mPaths.size)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = x
                mStartY = y
                actionDown(x, y)
            }
            MotionEvent.ACTION_MOVE -> actionMove(x, y)
            MotionEvent.ACTION_UP -> actionUp()
        }

        invalidate()
        return true
    }

    interface PathsChangedListener {
        fun pathsChanged(cnt: Int)
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
}
