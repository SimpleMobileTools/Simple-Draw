package com.simplemobiletools.draw.pro.extensions

import android.graphics.Rect
import android.view.View

val View.boundingBox: Rect
    get() {
        val rect = Rect()
        getDrawingRect(rect)
        val location = IntArray(2)
        getLocationOnScreen(location)
        rect.offset(location[0], location[1])
        return rect
    }

fun View.contains(x: Int, y: Int) = boundingBox.contains(x, y)
