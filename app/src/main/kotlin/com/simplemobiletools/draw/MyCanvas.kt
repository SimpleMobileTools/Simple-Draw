package com.simplemobiletools.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*

class MyCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val mPaint: Paint
    private var mPath: MyPath? = null
    private var mPaths: MutableMap<MyPath, PaintOptions>? = null
    private var mListener: PathsChangedListener? = null

    private var mPaintOptions: PaintOptions? = null
    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mIsSaving = false
    private var mIsStrokeWidthBarEnabled = false

    init {

        mPath = MyPath()
        mPaint = Paint()
        mPaintOptions = PaintOptions()
        mPaint.color = mPaintOptions!!.color
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mPaintOptions!!.strokeWidth
        mPaint.isAntiAlias = true

        mPaths = LinkedHashMap<MyPath, PaintOptions>()
        mPaths!!.put(mPath!!, mPaintOptions!!)
        pathsUpdated()
    }

    fun setListener(listener: PathsChangedListener) {
        this.mListener = listener
    }

    fun undo() {
        if (mPaths!!.isEmpty())
            return

        var lastKey: MyPath? = null
        for (key in mPaths!!.keys) {
            lastKey = key
        }

        mPaths!!.remove(lastKey)
        pathsUpdated()
        invalidate()
    }

    fun setColor(newColor: Int) {
        mPaintOptions!!.color = newColor
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun setStrokeWidth(newStrokeWidth: Float) {
        mPaintOptions!!.strokeWidth = newStrokeWidth
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun setIsStrokeWidthBarEnabled(isStrokeWidthBarEnabled: Boolean) {
        mIsStrokeWidthBarEnabled = isStrokeWidthBarEnabled
        invalidate()
    }

    val bitmap: Bitmap
        get() {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            mIsSaving = true
            draw(canvas)
            mIsSaving = false
            return bitmap
        }

    val paths: Map<MyPath, PaintOptions>
        get() = mPaths!!

    fun addPath(path: MyPath, options: PaintOptions) {
        mPaths!!.put(path, options)
        pathsUpdated()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for ((key, value) in mPaths!!) {
            changePaint(value)
            canvas.drawPath(key, mPaint)
        }

        changePaint(mPaintOptions!!)
        canvas.drawPath(mPath!!, mPaint)

        if (mIsStrokeWidthBarEnabled && !mIsSaving) {
            drawPreviewCircle(canvas)
        }
    }

    private fun drawPreviewCircle(canvas: Canvas) {
        val res = resources
        mPaint.style = Paint.Style.FILL

        var y = height - res.getDimension(R.dimen.preview_dot_offset_y)
        canvas.drawCircle((width / 2).toFloat(), y, mPaintOptions!!.strokeWidth / 2, mPaint)
        mPaint.style = Paint.Style.STROKE
        mPaint.color = if (Utils.shouldUseWhite(mPaintOptions!!.color)) Color.WHITE else Color.BLACK
        mPaint.strokeWidth = res.getDimension(R.dimen.preview_dot_stroke_size)

        y = height - res.getDimension(R.dimen.preview_dot_offset_y)
        val radius = (mPaintOptions!!.strokeWidth + res.getDimension(R.dimen.preview_dot_stroke_size)) / 2
        canvas.drawCircle((width / 2).toFloat(), y, radius, mPaint)
        changePaint(mPaintOptions!!)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
    }

    fun clearCanvas() {
        mPath!!.reset()
        mPaths!!.clear()
        pathsUpdated()
        invalidate()
    }

    private fun actionDown(x: Float, y: Float) {
        mPath!!.reset()
        mPath!!.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath!!.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        mPath!!.lineTo(mCurX, mCurY)

        // draw a dot on click
        if (mStartX == mCurX && mStartY == mCurY) {
            mPath!!.lineTo(mCurX, mCurY + 2)
            mPath!!.lineTo(mCurX + 1, mCurY + 2)
            mPath!!.lineTo(mCurX + 1, mCurY)
        }

        mPaths!!.put(mPath!!, mPaintOptions!!)
        pathsUpdated()
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions!!.color, mPaintOptions!!.strokeWidth)
    }

    private fun pathsUpdated() {
        if (mListener != null && mPaths != null) {
            mListener!!.pathsChanged(mPaths!!.size)
        }
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
            else -> {
            }
        }

        invalidate()
        return true
    }

    interface PathsChangedListener {
        fun pathsChanged(cnt: Int)
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)

        savedState.mPaths = mPaths
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val savedState = state
        super.onRestoreInstanceState(savedState.superState)

        mPaths = savedState.mPaths
        pathsUpdated()
    }

    internal class SavedState : View.BaseSavedState {
        var mPaths: MutableMap<MyPath, PaintOptions>? = null

        companion object {
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun newArray(size: Int): Array<SavedState> = arrayOf()

                override fun createFromParcel(source: Parcel) = SavedState(source)
            }
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(mPaths!!.size)
            for ((key, paintOptions) in mPaths!!) {
                out.writeSerializable(key)
                out.writeInt(paintOptions.color)
                out.writeFloat(paintOptions.strokeWidth)
            }
        }

        private constructor(parcel: Parcel) : super(parcel) {
            val size = parcel.readInt()
            for (i in 0..size - 1) {
                val key = parcel.readSerializable() as MyPath
                val paintOptions = PaintOptions(parcel.readInt(), parcel.readFloat())
                mPaths!!.put(key, paintOptions)
            }
        }
    }
}
