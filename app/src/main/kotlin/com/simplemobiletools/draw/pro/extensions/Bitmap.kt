package com.simplemobiletools.draw.pro.extensions

import android.graphics.Bitmap
import com.simplemobiletools.draw.pro.helpers.QueueLinearFloodFiller

fun Bitmap.floodFill(color: Int, x: Int, y: Int, tolerance: Int = 10): Bitmap {
    val floodFiller = QueueLinearFloodFiller(this).apply {
        fillColor = color
        setTolerance(tolerance)
    }

    floodFiller.floodFill(x, y)
    return floodFiller.image!!
}
