package com.simplemobiletools.draw.pro.helpers

import android.graphics.Bitmap
import android.graphics.Color
import java.util.*

// Original algorithm by J. Dunlap http:// www.codeproject.com/KB/GDI-plus/queuelinearflood-fill.aspx
// Java port by Owen Kaluza
// Android port by Darrin Smith (Standard Android)
class QueueLinearFloodFiller(img: Bitmap) {

    var image: Bitmap? = null
        private set

    var tolerance = intArrayOf(0, 0, 0)
    private var width = 0
    private var height = 0
    private var pixels: IntArray? = null
    var fillColor = 0
    private val startColor = intArrayOf(0, 0, 0)
    private lateinit var pixelsChecked: BooleanArray
    private var ranges: Queue<FloodFillRange>? = null

    init {
        copyImage(img)
    }

    fun setTargetColor(targetColor: Int) {
        startColor[0] = Color.red(targetColor)
        startColor[1] = Color.green(targetColor)
        startColor[2] = Color.blue(targetColor)
    }

    fun setTolerance(value: Int) {
        tolerance = intArrayOf(value, value, value)
    }

    private fun copyImage(img: Bitmap) {
        // Copy data from provided Image to a BufferedImage to write flood fill to, use getImage to retrieve
        // cache data in member variables to decrease overhead of property calls
        width = img.width
        height = img.height
        image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        image = img.copy(img.config, true)
        pixels = IntArray(width * height)
        image!!.getPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun prepare() {
        // Called before starting flood-fill
        pixelsChecked = BooleanArray(pixels!!.size)
        ranges = LinkedList()
    }

    //  Fills the specified point on the bitmap with the currently selected fill color.
    //  int x, int y: The starting coordinates for the fill
    fun floodFill(x: Int, y: Int) {
        // Setup
        prepare()
        if (startColor[0] == 0) {
            // ***Get starting color.
            val startPixel = pixels!![width * y + x]
            startColor[0] = startPixel shr 16 and 0xff
            startColor[1] = startPixel shr 8 and 0xff
            startColor[2] = startPixel and 0xff
        }

        // ***Do first call to flood-fill.
        linearFill(x, y)

        // ***Call flood-fill routine while flood-fill ranges still exist on the queue
        var range: FloodFillRange
        while (ranges!!.size > 0) {
            // **Get Next Range Off the Queue
            range = ranges!!.remove()

            // **Check Above and Below Each Pixel in the flood-fill Range
            var downPxIdx = width * (range.Y + 1) + range.startX
            var upPxIdx = width * (range.Y - 1) + range.startX
            val upY = range.Y - 1 // so we can pass the y coordinate by ref
            val downY = range.Y + 1
            for (i in range.startX..range.endX) {
                // *Start Fill Upwards
                // if we're not above the top of the bitmap and the pixel above this one is within the color tolerance
                if (range.Y > 0 && !pixelsChecked[upPxIdx] && checkPixel(upPxIdx)) {
                    linearFill(i, upY)
                }

                // *Start Fill Downwards
                // if we're not below the bottom of the bitmap and the pixel below this one is within the color tolerance
                if (range.Y < height - 1 && !pixelsChecked[downPxIdx] && checkPixel(downPxIdx)) {
                    linearFill(i, downY)
                }
                downPxIdx++
                upPxIdx++
            }
        }
        image!!.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    //  Finds the furthermost left and right boundaries of the fill area
    //  on a given y coordinate, starting from a given x coordinate, filling as it goes.
    //  Adds the resulting horizontal range to the queue of flood-fill ranges,
    //  to be processed in the main loop.
    // 
    //  int x, int y: The starting coordinates
    private fun linearFill(x: Int, y: Int) {
        // ***Find Left Edge of Color Area
        var lFillLoc = x // the location to check/fill on the left
        var pxIdx = width * y + x
        while (true) {
            // **fill with the color
            pixels!![pxIdx] = fillColor

            // **indicate that this pixel has already been checked and filled
            pixelsChecked[pxIdx] = true

            // **de-increment
            lFillLoc-- // de-increment counter
            pxIdx-- // de-increment pixel index

            // **exit loop if we're at edge of bitmap or color area
            if (lFillLoc < 0 || pixelsChecked[pxIdx] || !checkPixel(pxIdx)) {
                break
            }
        }
        lFillLoc++

        // ***Find Right Edge of Color Area
        var rFillLoc = x // the location to check/fill on the left
        pxIdx = width * y + x
        while (true) {
            // **fill with the color
            pixels!![pxIdx] = fillColor

            // **indicate that this pixel has already been checked and filled
            pixelsChecked[pxIdx] = true

            // **increment
            rFillLoc++ // increment counter
            pxIdx++ // increment pixel index

            // **exit loop if we're at edge of bitmap or color area
            if (rFillLoc >= width || pixelsChecked[pxIdx] || !checkPixel(pxIdx)) {
                break
            }
        }
        rFillLoc--

        // add range to queue
        val r = FloodFillRange(lFillLoc, rFillLoc, y)
        ranges!!.offer(r)
    }

    // Sees if a pixel is within the color tolerance range.
    private fun checkPixel(px: Int): Boolean {
        val red = pixels!![px] ushr 16 and 0xff
        val green = pixels!![px] ushr 8 and 0xff
        val blue = pixels!![px] and 0xff
        return red >= startColor[0] - tolerance[0] && red <= startColor[0] + tolerance[0] && green >= startColor[1] - tolerance[1] && green <= startColor[1] + tolerance[1] && blue >= startColor[2] - tolerance[2] && blue <= startColor[2] + tolerance[2]
    }

    //  Represents a linear range to be filled and branched from.
    private inner class FloodFillRange(var startX: Int, var endX: Int, var Y: Int)
}
