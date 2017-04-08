package com.simplemobiletools.draw

import android.graphics.Color

class PaintOptions {
    var color = Color.BLACK
    var strokeWidth = 5f

    constructor()

    constructor(color: Int, strokeWidth: Float) {
        this.color = color
        this.strokeWidth = strokeWidth
    }
}
