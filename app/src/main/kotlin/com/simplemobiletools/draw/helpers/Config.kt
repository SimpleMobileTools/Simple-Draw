package com.simplemobiletools.draw.helpers

import android.content.Context
import android.graphics.Color
import com.simplemobiletools.commons.helpers.BaseConfig

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showBrushSize: Boolean
        get() = prefs.getBoolean(SHOW_BRUSH_SIZE, false)
        set(showBrushSize) = prefs.edit().putBoolean(SHOW_BRUSH_SIZE, showBrushSize).apply()

    var brushColor: Int
        get() = prefs.getInt(BRUSH_COLOR, Color.BLACK)
        set(color) = prefs.edit().putInt(BRUSH_COLOR, color).apply()

    var brushSize: Float
        get() = prefs.getFloat(BRUSH_SIZE, 5.0f)
        set(brushSize) = prefs.edit().putFloat(BRUSH_SIZE, brushSize).apply()

    var canvasBackgroundColor: Int
        get() = prefs.getInt(CANVAS_BACKGROUND_COLOR, Color.WHITE)
        set(canvasBackgroundColor) = prefs.edit().putInt(CANVAS_BACKGROUND_COLOR, canvasBackgroundColor).apply()

    var lastSaveFolder: String
        get() = prefs.getString(LAST_SAVE_FOLDER, "")
        set(lastSaveFolder) = prefs.edit().putString(LAST_SAVE_FOLDER, lastSaveFolder).apply()
}
