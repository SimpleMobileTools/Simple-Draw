package com.simplemobiletools.draw.pro.activities

import android.os.Bundle
import android.view.Menu
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.draw.pro.R
import com.simplemobiletools.draw.pro.extensions.config
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupUseEnglish()
        setupPreventPhoneFromSleeping()
        setupBrushSize()
        setupAllowZoomingCanvas()
        setupForcePortraitMode()
        updateTextColors(settings_holder)
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupPreventPhoneFromSleeping() {
        settings_prevent_phone_from_sleeping.isChecked = config.preventPhoneFromSleeping
        settings_prevent_phone_from_sleeping_holder.setOnClickListener {
            settings_prevent_phone_from_sleeping.toggle()
            config.preventPhoneFromSleeping = settings_prevent_phone_from_sleeping.isChecked
        }
    }

    private fun setupBrushSize() {
        settings_show_brush_size.isChecked = config.showBrushSize
        settings_show_brush_size_holder.setOnClickListener {
            settings_show_brush_size.toggle()
            config.showBrushSize = settings_show_brush_size.isChecked
        }
    }

    private fun setupAllowZoomingCanvas() {
        settings_allow_zooming_canvas.isChecked = config.allowZoomingCanvas
        settings_allow_zooming_canvas_holder.setOnClickListener {
            settings_allow_zooming_canvas.toggle()
            config.allowZoomingCanvas = settings_allow_zooming_canvas.isChecked
        }
    }

    private fun setupForcePortraitMode() {
        settings_force_portrait.isChecked = config.forcePortraitMode
        settings_force_portrait_holder.setOnClickListener {
            settings_force_portrait.toggle()
            config.forcePortraitMode = settings_force_portrait.isChecked
        }
    }
}
