package com.simplemobiletools.draw

import android.graphics.Color

class PaintOptions {
    var color = Color.BLACK
    var strokeWidth = 5f
    var isEraser = false

    constructor()

    constructor(color: Int, strokeWidth: Float, isEraser: Boolean) {
        this.color = color
        this.strokeWidth = strokeWidth
        this.isEraser = isEraser
    }

    fun getColorToExport() = if (isEraser) "none" else Integer.toHexString(color).substring(2)
}
