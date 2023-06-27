package com.simplemobiletools.draw.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.isTiramisuPlus
import com.simplemobiletools.draw.pro.R
import com.simplemobiletools.draw.pro.extensions.config
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        updateMaterialActivityViews(settings_coordinator, settings_holder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(settings_nested_scrollview, settings_toolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(settings_toolbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupPreventPhoneFromSleeping()
        setupBrushSize()
        setupAllowZoomingCanvas()
        setupRelativeBrushSize()
        setupForcePortraitMode()
        updateTextColors(settings_holder)

        arrayOf(settings_color_customization_section_label, settings_general_settings_label).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupCustomizeColors() {
        settings_color_customization_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupLanguage() {
        settings_language.text = Locale.getDefault().displayLanguage
        settings_language_holder.beVisibleIf(isTiramisuPlus())
        settings_language_holder.setOnClickListener {
            launchChangeAppLanguageIntent()
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

    private fun setupRelativeBrushSize() {
        settings_relative_brush_size.isChecked = config.relativeBrushSize
        settings_relative_brush_size_holder.setOnClickListener {
            settings_relative_brush_size.toggle()
            config.relativeBrushSize = settings_relative_brush_size.isChecked
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
