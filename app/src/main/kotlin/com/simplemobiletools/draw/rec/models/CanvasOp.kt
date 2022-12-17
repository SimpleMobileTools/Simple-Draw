package com.simplemobiletools.draw.rec.models

import android.graphics.Bitmap
import java.io.Serializable

sealed class CanvasOp : Serializable {
    class PathOp(
        val path: MyPath,
        val paintOptions: PaintOptions
    ) : CanvasOp()

    class BitmapOp(
        val bitmap: Bitmap
    ) : CanvasOp()
}
