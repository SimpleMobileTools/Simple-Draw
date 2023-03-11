package com.simplemobiletools.draw.pro.extensions

import android.graphics.Bitmap
import com.simplemobiletools.draw.pro.helpers.VectorFloodFiller
import com.simplemobiletools.draw.pro.models.MyPath

fun Bitmap.vectorFloodFill(color: Int, x: Int, y: Int, tolerance: Int): MyPath {
    val floodFiller = VectorFloodFiller(this).apply {
        fillColor = color
        this.tolerance = tolerance
    }

    floodFiller.floodFill(x, y)
    return floodFiller.path
}
