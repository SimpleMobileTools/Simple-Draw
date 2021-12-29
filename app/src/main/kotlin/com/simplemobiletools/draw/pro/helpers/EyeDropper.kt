package com.simplemobiletools.draw.pro.helpers

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.View.DRAWING_CACHE_QUALITY_LOW
import android.widget.ImageView

// forked from  https://github.com/Madrapps/EyeDropper
class EyeDropper(private val view: View, private val onColorSelected: ((Int) -> Unit)) {
    companion object {
        private const val NO_COLOR = Color.TRANSPARENT
        private val INVERT_MATRIX = Matrix()
    }

    private var viewTouchListener = View.OnTouchListener { _, event ->
        notifyColorSelection(event.x.toInt(), event.y.toInt())
        true
    }

    fun start() {
        enableDrawingCache()
        view.setOnTouchListener(viewTouchListener)
    }

    fun stop() {
        disableDrawingCache()
        view.setOnTouchListener(null)
    }

    private fun enableDrawingCache() {
        if (view.shouldDrawingCacheBeEnabled()) {
            view.isDrawingCacheEnabled = true
            view.drawingCacheQuality = DRAWING_CACHE_QUALITY_LOW
        }
    }

    private fun disableDrawingCache() {
        if (view.shouldDrawingCacheBeEnabled()) {
            view.isDrawingCacheEnabled = false
        }
    }

    private fun notifyColorSelection(x: Int, y: Int) {
        val colorAtPoint = getColorAtPoint(x, y)
        onColorSelected.invoke(colorAtPoint)
    }

    private fun getColorAtPoint(x: Int, y: Int): Int {
        return when (view) {
            is ImageView -> handleIfImageView(view, x, y)
            else -> getPixelAtPoint(view.drawingCache, x, y)
        }
    }

    private fun handleIfImageView(view: ImageView, x: Int, y: Int): Int {
        return when (val drawable = view.drawable) {
            is BitmapDrawable -> {
                view.imageMatrix.invert(INVERT_MATRIX)
                val mappedPoints = floatArrayOf(x.toFloat(), y.toFloat())
                INVERT_MATRIX.mapPoints(mappedPoints)
                getPixelAtPoint(drawable.bitmap, mappedPoints[0].toInt(), mappedPoints[1].toInt())
            }
            else -> NO_COLOR
        }
    }

    private fun getPixelAtPoint(bitmap: Bitmap, x: Int, y: Int): Int {
        if (bitmap.isValidCoordinate(x, y)) {
            return bitmap.getPixel(x, y)
        }
        return NO_COLOR
    }

    private fun Bitmap.isValidCoordinate(x: Int, y: Int): Boolean {
        val isValidXCoordinate = x >= 1 && x < width
        val isValidYCoordinate = y >= 1 && y < height
        return isValidXCoordinate && isValidYCoordinate
    }

    private fun View.shouldDrawingCacheBeEnabled(): Boolean = (this !is ImageView) && !isDrawingCacheEnabled
}
