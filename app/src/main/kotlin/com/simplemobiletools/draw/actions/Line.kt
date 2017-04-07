package com.simplemobiletools.draw.actions

import android.graphics.Path

import java.io.IOException
import java.io.Writer
import java.security.InvalidParameterException

class Line : Action {

    val x: Float
    val y: Float

    @Throws(InvalidParameterException::class)
    constructor(data: String) {
        if (data.startsWith("L"))
            throw InvalidParameterException("The Line data should start with 'L'.")

        try {
            val xy = data.substring(1).split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            x = java.lang.Float.parseFloat(xy[0].trim { it <= ' ' })
            y = java.lang.Float.parseFloat(xy[1].trim { it <= ' ' })
        } catch (ignored: Exception) {
            throw InvalidParameterException("Error parsing the given Line data.")
        }

    }

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun perform(path: Path) {
        path.lineTo(x, y)
    }

    @Throws(IOException::class)
    override fun perform(writer: Writer) {
        writer.write("L")
        writer.write(x.toString())
        writer.write(",")
        writer.write(y.toString())
    }
}
