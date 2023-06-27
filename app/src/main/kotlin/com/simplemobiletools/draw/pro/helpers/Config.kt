package com.simplemobiletools.draw.pro.helpers

import android.content.Context
import android.graphics.Color
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.draw.pro.R

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showBrushSize: Boolean
        get() = prefs.getBoolean(SHOW_BRUSH_SIZE, true)
        set(showBrushSize) = prefs.edit().putBoolean(SHOW_BRUSH_SIZE, showBrushSize).apply()

    var brushColor: Int
        get() = prefs.getInt(BRUSH_COLOR, context.resources.getColor(R.color.color_primary))
        set(color) = prefs.edit().putInt(BRUSH_COLOR, color).apply()

    var brushSize: Float
        get() = prefs.getFloat(BRUSH_SIZE, 40f)
        set(brushSize) = prefs.edit().putFloat(BRUSH_SIZE, brushSize).apply()

    var canvasBackgroundColor: Int
        get() = prefs.getInt(CANVAS_BACKGROUND_COLOR, Color.WHITE)
        set(canvasBackgroundColor) = prefs.edit().putInt(CANVAS_BACKGROUND_COLOR, canvasBackgroundColor).apply()

    var lastSaveFolder: String
        get() = prefs.getString(LAST_SAVE_FOLDER, "")!!
        set(lastSaveFolder) = prefs.edit().putString(LAST_SAVE_FOLDER, lastSaveFolder).apply()

    var lastSaveExtension: String
        get() = prefs.getString(LAST_SAVE_EXTENSION, "")!!
        set(lastSaveExtension) = prefs.edit().putString(LAST_SAVE_EXTENSION, lastSaveExtension).apply()

    var allowZoomingCanvas: Boolean
        get() = prefs.getBoolean(ALLOW_ZOOMING_CANVAS, true)
        set(allowZoomingCanvas) = prefs.edit().putBoolean(ALLOW_ZOOMING_CANVAS, allowZoomingCanvas).apply()

    var relativeBrushSize: Boolean
        get() = prefs.getBoolean(RELATIVE_BRUSH_SIZE, true)
        set(relativeBrushSize) = prefs.edit().putBoolean(RELATIVE_BRUSH_SIZE, relativeBrushSize).apply()

    var forcePortraitMode: Boolean
        get() = prefs.getBoolean(FORCE_PORTRAIT_MODE, false)
        set(forcePortraitMode) = prefs.edit().putBoolean(FORCE_PORTRAIT_MODE, forcePortraitMode).apply()
}
