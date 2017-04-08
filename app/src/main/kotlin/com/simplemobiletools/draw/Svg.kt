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
        writer.write("<svg width=\"")
        writer.write(width.toString())
        writer.write("\" height=\"")
        writer.write(height.toString())
        writer.write("\" xmlns=\"http://www.w3.org/2000/svg\">")

        // background rect
        writer.write("<rect width=\"")
        writer.write(width.toString())
        writer.write("\" height=\"")
        writer.write(height.toString())
        writer.write("\" fill=\"#")
        writer.write(Integer.toHexString(backgroundColor).substring(2))
        writer.write("\"/>")

        for ((key, value) in paths) {
            writePath(writer, key, value)
        }
        writer.write("</svg>")
    }

    private fun writePath(writer: Writer, path: MyPath, options: PaintOptions) {
        writer.write("<path d=\"")
        for (action in path.getActions()) {
            action.perform(writer)
            writer.write(" ")
        }

        writer.write("\" fill=\"none\" stroke=\"#")
        writer.write(Integer.toHexString(options.color).substring(2))
        writer.write("\" stroke-width=\"")
        writer.write(options.strokeWidth.toString())
        writer.write("\" stroke-linecap=\"round\"/>")
    }

    fun loadSvg(file: File, canvas: MyCanvas) {
        val svg = parseSvg(file)

        canvas.clearCanvas()
        canvas.setBackgroundColor(svg.background!!.color)

        for (sp in svg.paths) {
            val path = MyPath()
            path.readObject(sp.data)
            val options = PaintOptions(sp.color, sp.strokeWidth)

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
                val width = Integer.parseInt(attributes.getValue("width"))
                val height = Integer.parseInt(attributes.getValue("height"))
                svg.setSize(width, height)
            }

            rectElement.setStartElementListener { attributes ->
                val width = Integer.parseInt(attributes.getValue("width"))
                val height = Integer.parseInt(attributes.getValue("height"))
                val color = Color.parseColor(attributes.getValue("fill"))
                if (svg.background != null)
                    throw UnsupportedOperationException("Unsupported SVG, should only have one <rect>.")

                svg.background = SRect(width, height, color)
            }

            pathElement.setStartElementListener { attributes ->
                val d = attributes.getValue("d")
                val color = Color.parseColor(attributes.getValue("stroke"))
                val width = java.lang.Float.parseFloat(attributes.getValue("stroke-width"))
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
