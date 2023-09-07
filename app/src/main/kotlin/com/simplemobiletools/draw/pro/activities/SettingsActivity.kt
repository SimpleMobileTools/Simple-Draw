package com.simplemobiletools.draw.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.isTiramisuPlus
import com.simplemobiletools.draw.pro.databinding.ActivitySettingsBinding
import com.simplemobiletools.draw.pro.extensions.config
import java.util.Locale

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            updateMaterialActivityViews(settingsCoordinator, settingsHolder, useTransparentNavigation = true, useTopSearchMenu = false)
            setupMaterialScrollListener(settingsNestedScrollview, settingsToolbar)
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupPreventPhoneFromSleeping()
        setupBrushSize()
        setupAllowZoomingCanvas()
        setupForcePortraitMode()
        updateTextColors(binding.settingsHolder)

        arrayOf(binding.settingsColorCustomizationSectionLabel, binding.settingsGeneralSettingsLabel).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        binding.apply {
            settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
            settingsUseEnglish.isChecked = config.useEnglish
            settingsUseEnglishHolder.setOnClickListener {
                settingsUseEnglish.toggle()
                config.useEnglish = settingsUseEnglish.isChecked
                System.exit(0)
            }
        }
    }

    private fun setupLanguage() {
        binding.apply {
            settingsLanguage.text = Locale.getDefault().displayLanguage
            settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
            settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        }
    }

    private fun setupPreventPhoneFromSleeping() {
        binding.apply {
            settingsPreventPhoneFromSleeping.isChecked = config.preventPhoneFromSleeping
            settingsPreventPhoneFromSleepingHolder.setOnClickListener {
                settingsPreventPhoneFromSleeping.toggle()
                config.preventPhoneFromSleeping = settingsPreventPhoneFromSleeping.isChecked
            }
        }
    }

    private fun setupBrushSize() {
        binding.apply {
            settingsShowBrushSize.isChecked = config.showBrushSize
            settingsShowBrushSizeHolder.setOnClickListener {
                settingsShowBrushSize.toggle()
                config.showBrushSize = settingsShowBrushSize.isChecked
            }
        }
    }

    private fun setupAllowZoomingCanvas() {
        binding.apply {
            settingsAllowZoomingCanvas.isChecked = config.allowZoomingCanvas
            settingsAllowZoomingCanvasHolder.setOnClickListener {
                settingsAllowZoomingCanvas.toggle()
                config.allowZoomingCanvas = settingsAllowZoomingCanvas.isChecked
            }
        }
    }

    private fun setupForcePortraitMode() {
        binding.apply {
            settingsForcePortrait.isChecked = config.forcePortraitMode
            settingsForcePortraitHolder.setOnClickListener {
                settingsForcePortrait.toggle()
                config.forcePortraitMode = settingsForcePortrait.isChecked
            }
        }
    }
}
