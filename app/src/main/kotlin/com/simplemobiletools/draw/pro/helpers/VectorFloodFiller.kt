package com.simplemobiletools.draw.pro.helpers

import android.graphics.Bitmap
import android.graphics.Color
import com.simplemobiletools.draw.pro.models.MyPath
import java.util.LinkedList
import java.util.Queue

// Original algorithm by J. Dunlap http:// www.codeproject.com/KB/GDI-plus/queuelinearflood-fill.aspx
// Java port by Owen Kaluza
// Android port by Darrin Smith (Standard Android)
class VectorFloodFiller(image: Bitmap) {
    val path = MyPath()

    private val width: Int
    private val height: Int
    private val pixels: IntArray

    private lateinit var pixelsChecked: BooleanArray
    private lateinit var ranges: Queue<FloodFillRange>

    var fillColor = 0
    var tolerance = 0
    private var startColorRed = 0
    private var startColorGreen = 0
    private var startColorBlue = 0

    init {
        width = image.width
        height = image.height
        pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun prepare() {
        // Called before starting flood-fill
        pixelsChecked = BooleanArray(pixels.size)
        ranges = LinkedList()
    }

    //  Fills the specified point on the bitmap with the currently selected fill color.
    //  int x, int y: The starting coordinates for the fill
    fun floodFill(x: Int, y: Int) {
        // Setup
        prepare()

        // Get starting color.
        val startPixel = pixels.getOrNull(width * y + x) ?: return
        if (startPixel == fillColor) {
            // No-op.
            return
        }
        startColorRed = Color.red(startPixel)
        startColorGreen = Color.green(startPixel)
        startColorBlue = Color.blue(startPixel)

        // Do first call to flood-fill.
        linearFill(x, y)

        // Call flood-fill routine while flood-fill ranges still exist on the queue
        var range: FloodFillRange
        while (ranges.size > 0) {
            // Get Next Range Off the Queue
            range = ranges.remove()

            // Check Above and Below Each Pixel in the flood-fill Range
            var downPxIdx = width * (range.Y + 1) + range.startX
            var upPxIdx = width * (range.Y - 1) + range.startX
            val upY = range.Y - 1 // so we can pass the y coordinate by ref
            val downY = range.Y + 1
            for (i in range.startX..range.endX) {
                // Start Fill Upwards
                // if we're not above the top of the bitmap and the pixel above this one is within the color tolerance
                if (range.Y > 0 && !pixelsChecked[upPxIdx] && isPixelColorWithinTolerance(upPxIdx)) {
                    linearFill(i, upY)
                }

                // Start Fill Downwards
                // if we're not below the bottom of the bitmap and the pixel below this one is within the color tolerance
                if (range.Y < height - 1 && !pixelsChecked[downPxIdx] && isPixelColorWithinTolerance(downPxIdx)) {
                    linearFill(i, downY)
                }
                downPxIdx++
                upPxIdx++
            }
        }
    }

    //  Finds the furthermost left and right boundaries of the fill area
    //  on a given y coordinate, starting from a given x coordinate, filling as it goes.
    //  Adds the resulting horizontal range to the queue of flood-fill ranges,
    //  to be processed in the main loop.
    //
    //  int x, int y: The starting coordinates
    private fun linearFill(x: Int, y: Int) {
        // Find Left Edge of Color Area
        var lFillLoc = x // the location to check/fill on the left
        var pxIdx = width * y + x
        path.moveTo(x.toFloat(), y.toFloat())
        while (true) {
            pixelsChecked[pxIdx] = true
            lFillLoc--
            pxIdx--
            // exit loop if we're at edge of bitmap or color area
            if (lFillLoc < 0 || pixelsChecked[pxIdx] || !isPixelColorWithinTolerance(pxIdx)) {
                break
            }
        }
        vectorFill(pxIdx + 1)
        lFillLoc++

        // Find Right Edge of Color Area
        var rFillLoc = x // the location to check/fill on the left
        pxIdx = width * y + x
        while (true) {
            pixelsChecked[pxIdx] = true
            rFillLoc++
            pxIdx++
            if (rFillLoc >= width || pixelsChecked[pxIdx] || !isPixelColorWithinTolerance(pxIdx)) {
                break
            }
        }
        vectorFill(pxIdx - 1)
        rFillLoc--

        // add range to queue
        val r = FloodFillRange(lFillLoc, rFillLoc, y)
        ranges.offer(r)
    }

    // vector fill pixels with color
    private fun vectorFill(pxIndex: Int) {
        val x = (pxIndex % width).toFloat()
        val y = (pxIndex - x) / width
        path.lineTo(x, y)
    }

    // Sees if a pixel is within the color tolerance range.
    private fun isPixelColorWithinTolerance(px: Int): Boolean {
        val red = pixels[px] ushr 16 and 0xff
        val green = pixels[px] ushr 8 and 0xff
        val blue = pixels[px] and 0xff
        return red >= startColorRed - tolerance && red <= startColorRed + tolerance && green >= startColorGreen - tolerance && green <= startColorGreen + tolerance && blue >= startColorBlue - tolerance && blue <= startColorBlue + tolerance
    }

    //  Represents a linear range to be filled and branched from.
    private inner class FloodFillRange(var startX: Int, var endX: Int, var Y: Int)
}
