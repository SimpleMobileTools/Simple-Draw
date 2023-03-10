package com.simplemobiletools.draw.pro.extensions

import android.graphics.Bitmap
import com.simplemobiletools.draw.pro.helpers.QueueLinearFloodFiller
import com.simplemobiletools.draw.pro.helpers.VectorFloodFiller
import com.simplemobiletools.draw.pro.models.MyPath

fun Bitmap.floodFill(color: Int, x: Int, y: Int, tolerance: Int = 10): Bitmap {
    val floodFiller = QueueLinearFloodFiller(this).apply {
        fillColor = color
        setTolerance(tolerance)
    }

    floodFiller.floodFill(x, y)
    return floodFiller.image!!
}

fun Bitmap.vectorFloodFill(color: Int, x: Int, y: Int, tolerance: Int): MyPath {
    val floodFiller = VectorFloodFiller(this).apply {
        fillColor = color
        this.tolerance = tolerance
    }

    floodFiller.floodFill(x, y)
    return floodFiller.path
}
