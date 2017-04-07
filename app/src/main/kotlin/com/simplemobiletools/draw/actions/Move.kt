package com.simplemobiletools.draw.actions

import android.graphics.Path

import java.io.IOException
import java.io.Writer
import java.security.InvalidParameterException

class Move : Action {

    val x: Float
    val y: Float

    @Throws(InvalidParameterException::class)
    constructor(data: String) {
        if (data.startsWith("M"))
            throw InvalidParameterException("The Move data should start with 'M'.")

        try {
            val xy = data.substring(1).split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            x = java.lang.Float.parseFloat(xy[0].trim { it <= ' ' })
            y = java.lang.Float.parseFloat(xy[1].trim { it <= ' ' })
        } catch (ignored: Exception) {
            throw InvalidParameterException("Error parsing the given Move data.")
        }

    }

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun perform(path: Path) {
        path.moveTo(x, y)
    }

    @Throws(IOException::class)
    override fun perform(writer: Writer) {
        writer.write("M")
        writer.write(x.toString())
        writer.write(",")
        writer.write(y.toString())
    }
}
