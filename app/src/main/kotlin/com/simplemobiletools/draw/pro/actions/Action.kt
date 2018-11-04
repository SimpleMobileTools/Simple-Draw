package com.simplemobiletools.draw.pro.actions

import android.graphics.Path
import java.io.Serializable
import java.io.Writer

interface Action : Serializable {
    fun perform(path: Path)

    fun perform(writer: Writer)
}
