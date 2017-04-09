package com.simplemobiletools.draw

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.sax.RootElement
import android.util.Xml
import java.io.*
import java.util.*

object Svg {
    fun saveSvg(output: File, canvas: MyCanvas) {
        val backgroundColor = (canvas.background as ColorDrawable).color

        val out = FileOutputStream(output)
        val writer = BufferedWriter(OutputStreamWriter(out))
        writeSvg(writer, backgroundColor, canvas.mPaths, canvas.width, canvas.height)
        writer.close()
    }

    private fun writeSvg(writer: Writer, backgroundColor: Int, paths: Map<MyPath, PaintOptions>, width: Int, height: Int) {
        writer.apply {
            write("<svg width=\"")
            write(width.toString())
            write("\" height=\"")
            write(height.toString())
            write("\" xmlns=\"http://www.w3.org/2000/svg\">")

            // background rect
            write("<rect width=\"")
            write(width.toString())
            write("\" height=\"")
            write(height.toString())
            write("\" fill=\"#")
            write(Integer.toHexString(backgroundColor).substring(2))
            write("\"/>")

            for ((key, value) in paths) {
                writePath(this, key, value)
            }
            write("</svg>")
        }
    }

    private fun writePath(writer: Writer, path: MyPath, options: PaintOptions) {
        writer.apply {
            write("<path d=\"")
            path.getActions().forEach {
                it.perform(this)
                write(" ")
            }

            write("\" fill=\"none\" stroke=\"#")
            write(Integer.toHexString(options.color).substring(2))
            write("\" stroke-width=\"")
            write(options.strokeWidth.toString())
            write("\" stroke-linecap=\"round\"/>")
        }
    }

    fun loadSvg(file: File, canvas: MyCanvas) {
        val svg = parseSvg(file)

        canvas.clearCanvas()
        canvas.setBackgroundColor(svg.background!!.color)

        svg.paths.forEach {
            val path = MyPath()
            path.readObject(it.data)
            val options = PaintOptions(it.color, it.strokeWidth)

            canvas.addPath(path, options)
        }
    }

    private fun parseSvg(file: File): SSvg {
        var inputStream: InputStream? = null
        val svg = SSvg()
        try {
            inputStream = FileInputStream(file)

            // Actual parsing (http://stackoverflow.com/a/4828765)
            val ns = "http://www.w3.org/2000/svg"
            val root = RootElement(ns, "svg")
            val rectElement = root.getChild(ns, "rect")
            val pathElement = root.getChild(ns, "path")

            root.setStartElementListener { attributes ->
                val width = attributes.getValue("width").toInt()
                val height = attributes.getValue("height").toInt()
                svg.setSize(width, height)
            }

            rectElement.setStartElementListener { attributes ->
                val width = attributes.getValue("width").toInt()
                val height = attributes.getValue("height").toInt()
                val color = Color.parseColor(attributes.getValue("fill"))
                if (svg.background != null)
                    throw UnsupportedOperationException("Unsupported SVG, should only have one <rect>.")

                svg.background = SRect(width, height, color)
            }

            pathElement.setStartElementListener { attributes ->
                val d = attributes.getValue("d")
                val color = Color.parseColor(attributes.getValue("stroke"))
                val width = attributes.getValue("stroke-width").toFloat()
                svg.paths.add(SPath(d, color, width))
            }

            Xml.parse(inputStream, Xml.Encoding.UTF_8, root.contentHandler)
        } finally {
            inputStream?.close()
        }
        return svg
    }

    private class SSvg : Serializable {
        var background: SRect? = null
        val paths: ArrayList<SPath> = ArrayList()
        private var width = 0
        private var height = 0

        internal fun setSize(w: Int, h: Int) {
            width = w
            height = h
        }
    }

    private class SRect(val width: Int, val height: Int, val color: Int) : Serializable

    private class SPath(var data: String, var color: Int, var strokeWidth: Float) : Serializable
}
