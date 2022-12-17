package com.simplemobiletools.draw.rec.models

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.sax.RootElement
import android.util.Xml
import com.simplemobiletools.commons.extensions.getFileOutputStream
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.draw.rec.R
import com.simplemobiletools.draw.rec.activities.MainActivity
import com.simplemobiletools.draw.rec.activities.SimpleActivity
import com.simplemobiletools.draw.rec.views.MyCanvas
import java.io.*

object Svg {
    fun saveSvg(activity: SimpleActivity, path: String, canvas: MyCanvas) {
        activity.getFileOutputStream(FileDirItem(path, path.getFilenameFromPath()), true) {
            saveToOutputStream(activity, it, canvas)
        }
    }

    fun saveToOutputStream(activity: SimpleActivity, outputStream: OutputStream?, canvas: MyCanvas) {
        if (outputStream != null) {
            val backgroundColor = (canvas.background as ColorDrawable).color
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            writeSvg(writer, backgroundColor, canvas.getPathsMap(), canvas.width, canvas.height)
            writer.close()
            activity.toast(R.string.file_saved)
        } else {
            activity.toast(R.string.unknown_error_occurred)
        }
    }

    private fun writeSvg(writer: Writer, backgroundColor: Int, paths: Map<MyPath, PaintOptions>, width: Int, height: Int) {
        writer.apply {
            write("<svg width=\"$width\" height=\"$height\" xmlns=\"http://www.w3.org/2000/svg\">")
            write("<rect width=\"$width\" height=\"$height\" fill=\"#${Integer.toHexString(backgroundColor).substring(2)}\"/>")

            for ((key, value) in paths) {
                writePath(this, key, value)
            }
            write("</svg>")
        }
    }

    private fun writePath(writer: Writer, path: MyPath, options: PaintOptions) {
        writer.apply {
            write("<path d=\"")
            path.actions.forEach {
                it.perform(this)
                write(" ")
            }

            write("\" fill=\"none\" stroke=\"")
            write(options.getColorToExport())
            write("\" stroke-width=\"")
            write(options.strokeWidth.toString())
            write("\" stroke-linecap=\"round\"/>")
        }
    }

    fun loadSvg(activity: MainActivity, fileOrUri: Any, canvas: MyCanvas) {
        val svg = parseSvg(activity, fileOrUri)

        canvas.clearCanvas()
        activity.setBackgroundColor(svg.background!!.color)

        svg.paths.forEach {
            val path = MyPath()
            path.readObject(it.data, activity)
            val options = PaintOptions(it.color, it.strokeWidth, it.isEraser)

            canvas.addPath(path, options)
        }
    }

    private fun parseSvg(activity: MainActivity, fileOrUri: Any): SSvg {
        var inputStream: InputStream? = null
        val svg = SSvg()
        try {
            inputStream = when (fileOrUri) {
                is File -> FileInputStream(fileOrUri)
                is Uri -> activity.contentResolver.openInputStream(fileOrUri)
                else -> null
            }

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
                val width = attributes.getValue("stroke-width").toFloat()
                val stroke = attributes.getValue("stroke")
                val isEraser = stroke == "none"
                val color = if (isEraser) 0 else Color.parseColor(stroke)
                svg.paths.add(SPath(d, color, width, isEraser))
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

    private class SPath(var data: String, var color: Int, var strokeWidth: Float, var isEraser: Boolean) : Serializable
}
