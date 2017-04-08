package com.simplemobiletools.draw.actions

import android.graphics.Path

import java.io.IOException
import java.io.Serializable
import java.io.Writer

interface Action : Serializable {
    fun perform(path: Path)

    @Throws(IOException::class)
    fun perform(writer: Writer)
}
