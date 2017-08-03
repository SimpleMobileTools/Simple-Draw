package com.simplemobiletools.draw

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.extensions.getContrastColor
import java.util.*

class MyCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var mPaths = LinkedHashMap<MyPath, PaintOptions>()
    var mBackgroundBitmap: Bitmap? = null
    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var mListener: PathsChangedListener? = null
    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mIsSaving = false
    private var mIsStrokeWidthBarEnabled = false

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

    fun setListener(listener: PathsChangedListener) {
        this.mListener = listener
    }

    fun undo() {
        if (mPaths.isEmpty())
            return

        val lastKey = mPaths.keys.lastOrNull()

        mPaths.remove(lastKey)
        pathsUpdated()
        invalidate()
    }

    fun setColor(newColor: Int) {
        mPaintOptions.color = newColor
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
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

    fun drawBitmap(activity: Activity, path: String) {
        Thread({
            val size = Point()
            activity.windowManager.defaultDisplay.getSize(size)
            val options = RequestOptions()
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .fitCenter()

            val builder = Glide.with(context)
                    .asBitmap()
                    .load(path)
                    .apply(options)
                    .into(size.x, size.y)

            mBackgroundBitmap = builder.get()
            activity.runOnUiThread {
                invalidate()
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
        mPaint.color = mPaintOptions.color.getContrastColor()
        mPaint.strokeWidth = res.getDimension(R.dimen.preview_dot_stroke_size)

        y = height - res.getDimension(R.dimen.preview_dot_offset_y)
        val radius = (mPaintOptions.strokeWidth + res.getDimension(R.dimen.preview_dot_stroke_size)) / 2
        canvas.drawCircle((width / 2).toFloat(), y, radius, mPaint)
        changePaint(mPaintOptions)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
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
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth)
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

    internal class MyParcelable : View.BaseSavedState {
        var paths = LinkedHashMap<MyPath, PaintOptions>()

        constructor(superState: Parcelable) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            val size = parcel.readInt()
            for (i in 0..size - 1) {
                val key = parcel.readSerializable() as MyPath
                val paintOptions = PaintOptions(parcel.readInt(), parcel.readFloat())
                paths.put(key, paintOptions)
            }
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(paths.size)
            for ((path, paintOptions) in paths) {
                out.writeSerializable(path)
                out.writeInt(paintOptions.color)
                out.writeFloat(paintOptions.strokeWidth)
            }
        }

        companion object {
            val CREATOR: Parcelable.Creator<MyParcelable> = object : Parcelable.Creator<MyParcelable> {
                override fun createFromParcel(source: Parcel) = MyParcelable(source)

                override fun newArray(size: Int) = arrayOf<MyParcelable>()
            }
        }
    }
}
