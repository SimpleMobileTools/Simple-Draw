package com.simplemobiletools.draw.actions

import android.graphics.Path
import java.io.Writer
import java.security.InvalidParameterException

class Quad : Action {

    val x1: Float
    val y1: Float
    val x2: Float
    val y2: Float

    constructor(data: String) {
        if (!data.startsWith("Q"))
            throw InvalidParameterException("The Quad data should start with 'Q'.")

        try {
            val parts = data.split("\\s+".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            val xy1 = parts[0].substring(1).split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            val xy2 = parts[1].split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray() // No need to skip the 'Q' here

            x1 = java.lang.Float.parseFloat(xy1[0].trim { it <= ' ' })
            y1 = java.lang.Float.parseFloat(xy1[1].trim { it <= ' ' })
            x2 = java.lang.Float.parseFloat(xy2[0].trim { it <= ' ' })
            y2 = java.lang.Float.parseFloat(xy2[1].trim { it <= ' ' })
        } catch (ignored: Exception) {
            throw InvalidParameterException("Error parsing the given Quad data.")
        }

    }

    constructor(x1: Float, y1: Float, x2: Float, y2: Float) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }

    override fun perform(path: Path) {
        path.quadTo(x1, y1, x2, y2)
    }

    override fun perform(writer: Writer) {
        writer.write("Q")
        writer.write(x1.toString())
        writer.write(",")
        writer.write(y1.toString())
        writer.write(" ")
        writer.write(x2.toString())
        writer.write(",")
        writer.write(y2.toString())
    }
}
