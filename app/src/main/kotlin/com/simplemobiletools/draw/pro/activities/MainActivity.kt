package com.simplemobiletools.draw.pro.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import androidx.print.PrintHelper
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LICENSE_GLIDE
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.SAVE_DISCARD_PROMPT_INTERVAL
import com.simplemobiletools.commons.helpers.isQPlus
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.draw.pro.BuildConfig
import com.simplemobiletools.draw.pro.R
import com.simplemobiletools.draw.pro.databinding.ActivityMainBinding
import com.simplemobiletools.draw.pro.dialogs.SaveImageDialog
import com.simplemobiletools.draw.pro.extensions.config
import com.simplemobiletools.draw.pro.helpers.EyeDropper
import com.simplemobiletools.draw.pro.helpers.JPG
import com.simplemobiletools.draw.pro.helpers.PNG
import com.simplemobiletools.draw.pro.helpers.SVG
import com.simplemobiletools.draw.pro.interfaces.CanvasListener
import com.simplemobiletools.draw.pro.models.Svg
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

class MainActivity : SimpleActivity(), CanvasListener {
    companion object {
        private const val PICK_IMAGE_INTENT = 1
        private const val SAVE_IMAGE_INTENT = 2

        private const val FOLDER_NAME = "images"
        private const val FILE_NAME = "simple-draw.png"
        private const val BITMAP_PATH = "bitmap_path"
        private const val URI_TO_LOAD = "uri_to_load"
    }

    private val binding by lazy(LazyThreadSafetyMode.NONE) { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var eyeDropper: EyeDropper

    private var defaultPath = ""
    private var defaultFilename = ""
    private var defaultExtension = PNG

    private var intentUri: Uri? = null
    private var uriToLoad: Uri? = null
    private var color = 0
    private var brushSize = 0f
    private var savedPathsHash = 0L
    private var lastSavePromptTS = 0L
    private var isEraserOn = false
    private var isEyeDropperOn = false
    private var isBucketFillOn = false
    private var isImageCaptureIntent = false
    private var isEditIntent = false
    private var lastBitmapPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        eyeDropper = EyeDropper(binding.myCanvas) { selectedColor ->
            setColor(selectedColor)
        }

        binding.myCanvas.mListener = this
        binding.strokeWidthBar.onSeekBarChangeListener { progress ->
            brushSize = Math.max(progress.toFloat(), 5f)
            updateBrushSize()
        }

        setBackgroundColor(config.canvasBackgroundColor)
        setColor(config.brushColor)
        defaultPath = config.lastSaveFolder
        defaultExtension = config.lastSaveExtension

        brushSize = config.brushSize
        updateBrushSize()
        binding.strokeWidthBar.progress = brushSize.toInt()

        binding.apply {
            colorPicker.setOnClickListener { pickColor() }
            undo.setOnClickListener { myCanvas.undo() }
            undo.setOnLongClickListener {
                toast(R.string.undo)
                true
            }

            eraser.setOnClickListener { eraserClicked() }
            eraser.setOnLongClickListener {
                toast(R.string.eraser)
                true
            }

            redo.setOnClickListener { myCanvas.redo() }
            redo.setOnLongClickListener {
                toast(R.string.redo)
                true
            }

            eyeDropper.setOnClickListener { eyeDropperClicked() }
            eyeDropper.setOnLongClickListener {
                toast(R.string.eyedropper)
                true
            }

            bucketFill.setOnClickListener { bucketFillClicked() }
            bucketFill.setOnLongClickListener {
                toast(R.string.bucket_fill)
                true
            }
        }

        checkIntents()
        if (!isImageCaptureIntent) {
            checkWhatsNewDialog()
        }

        if (isPackageInstalled("com.simplemobiletools.draw")) {
            ConfirmationDialog(this, "", R.string.upgraded_from_free, R.string.ok, 0, false) {}
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.mainToolbar, statusBarColor = getProperBackgroundColor())

        binding.apply {
            val isShowBrushSizeEnabled = config.showBrushSize
            strokeWidthBar.beVisibleIf(isShowBrushSizeEnabled)
            strokeWidthPreview.beVisibleIf(isShowBrushSizeEnabled)
            myCanvas.setAllowZooming(config.allowZoomingCanvas)
            updateTextColors(mainHolder)
            if (isBlackAndWhiteTheme()) {
                strokeWidthBar.setColors(0, config.canvasBackgroundColor.getContrastColor(), 0)
            }
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        requestedOrientation = if (config.forcePortraitMode) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        refreshMenuItems()
        updateButtonStates()
    }

    override fun onPause() {
        super.onPause()
        config.brushColor = color
        config.brushSize = brushSize
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.myCanvas.mListener = null
    }

    private fun refreshMenuItems() {
        binding.mainToolbar.menu.apply {
            findItem(R.id.menu_confirm).isVisible = isImageCaptureIntent || isEditIntent
            findItem(R.id.menu_save).isVisible = !isImageCaptureIntent && !isEditIntent
            findItem(R.id.menu_share).isVisible = !isImageCaptureIntent && !isEditIntent
            findItem(R.id.open_file).isVisible = !isEditIntent
            findItem(R.id.more_apps_from_us).isVisible = !resources.getBoolean(R.bool.hide_google_relations)
        }
    }

    private fun setupOptionsMenu() {
        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_confirm -> confirmImage()
                R.id.menu_save -> trySaveImage()
                R.id.menu_share -> shareImage()
                R.id.clear -> clearCanvas()
                R.id.open_file -> tryOpenFile()
                R.id.change_background -> changeBackgroundClicked()
                R.id.menu_print -> printImage()
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onBackPressed() {
        val hasUnsavedChanges = savedPathsHash != binding.myCanvas.getDrawingHashCode()
        if (hasUnsavedChanges && System.currentTimeMillis() - lastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL) {
            lastSavePromptTS = System.currentTimeMillis()
            ConfirmationAdvancedDialog(this, "", R.string.save_before_closing, R.string.save, R.string.discard) {
                if (it) {
                    trySaveImage()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_GLIDE

        val faqItems = ArrayList<FAQItem>()

        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))
            faqItems.add(FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons))
            faqItems.add(FAQItem(R.string.faq_7_title_commons, R.string.faq_7_text_commons))
            faqItems.add(FAQItem(R.string.faq_10_title_commons, R.string.faq_10_text_commons))
        }

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_IMAGE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryOpenUri(resultData.data!!, resultData)
        } else if (requestCode == SAVE_IMAGE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            if (defaultExtension == SVG) {
                Svg.saveToOutputStream(this, outputStream, binding.myCanvas)
            } else {
                saveToOutputStream(outputStream, defaultExtension.getCompressionFormat(), false)
            }
            savedPathsHash = binding.myCanvas.getDrawingHashCode()
        }
    }

    private fun tryOpenFile() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"

            try {
                startActivityForResult(this, PICK_IMAGE_INTENT)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun checkIntents() {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            tryOpenUri(uri!!, intent)
        }

        if (intent?.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("image/") == true) {
            val imageUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!
            imageUris.any { tryOpenUri(it, intent) }
        }

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            tryOpenUri(intent.data!!, intent)
        }

        if (intent?.action == MediaStore.ACTION_IMAGE_CAPTURE) {
            val output = intent.extras?.get(MediaStore.EXTRA_OUTPUT)
            if (output != null && output is Uri) {
                isImageCaptureIntent = true
                intentUri = output
                defaultPath = output.path!!
                refreshMenuItems()
            }
        }

        if (intent?.action == Intent.ACTION_EDIT) {
            val data = intent.data
            val output = intent.extras?.get(MediaStore.EXTRA_OUTPUT)
            if (data != null && output != null && output is Uri) {
                tryOpenUri(data, intent)
                isEditIntent = true
                intentUri = output
            }
        }
    }

    private fun getStoragePermission(callback: () -> Unit) {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                callback()
            } else {
                toast(R.string.no_storage_permissions)
            }
        }
    }

    private fun tryOpenUri(uri: Uri, intent: Intent) = when {
        uri.scheme == "file" -> {
            uriToLoad = uri
            openPath(uri.path!!)
        }

        uri.scheme == "content" -> {
            uriToLoad = uri
            openUri(uri, intent)
        }

        else -> false
    }

    private fun openPath(path: String) = when {
        path.endsWith(".svg") -> {
            binding.myCanvas.mBackgroundBitmap = null
            Svg.loadSvg(this, File(path), binding.myCanvas)
            defaultExtension = SVG
            true
        }

        File(path).isImageSlow() -> {
            lastBitmapPath = path
            binding.myCanvas.drawBitmap(this, path)
            defaultExtension = JPG
            true
        }

        else -> {
            toast(R.string.invalid_file_format)
            false
        }
    }

    private fun openUri(uri: Uri, intent: Intent): Boolean {
        val mime = MimeTypeMap.getSingleton()
        val type = mime.getExtensionFromMimeType(contentResolver.getType(uri)) ?: intent.type ?: contentResolver.getType(uri)
        return when (type) {
            "svg", "image/svg+xml" -> {
                binding.myCanvas.mBackgroundBitmap = null
                Svg.loadSvg(this, uri, binding.myCanvas)
                defaultExtension = SVG
                true
            }

            "jpg", "jpeg", "png", "gif", "image/jpg", "image/png", "image/gif", "webp" -> {
                binding.myCanvas.drawBitmap(this, uri)
                defaultExtension = JPG
                true
            }

            else -> {
                toast(R.string.invalid_file_format)
                false
            }
        }
    }

    private fun eraserClicked() {
        if (isEyeDropperOn) {
            eyeDropperClicked()
        } else if (isBucketFillOn) {
            bucketFillClicked()
        }

        isEraserOn = !isEraserOn
        updateEraserState()
    }

    private fun updateEraserState() {
        updateButtonStates()
        binding.myCanvas.toggleEraser(isEraserOn)
    }

    private fun changeBackgroundClicked() {
        val oldColor = (binding.myCanvas.background as ColorDrawable).color
        ColorPickerDialog(this, oldColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                config.canvasBackgroundColor = color
                setBackgroundColor(color)

                if (uriToLoad != null) {
                    tryOpenUri(uriToLoad!!, intent)
                }
            }
        }
    }

    private fun eyeDropperClicked() {
        if (isEraserOn) {
            eraserClicked()
        } else if (isBucketFillOn) {
            bucketFillClicked()
        }

        isEyeDropperOn = !isEyeDropperOn
        if (isEyeDropperOn) {
            eyeDropper.start()
        } else {
            eyeDropper.stop()
        }

        updateButtonStates()
    }

    private fun bucketFillClicked() {
        if (isEraserOn) {
            eraserClicked()
        } else if (isEyeDropperOn) {
            eyeDropperClicked()
        }

        isBucketFillOn = !isBucketFillOn

        updateButtonStates()
        binding.myCanvas.toggleBucketFill(isBucketFillOn)
    }

    private fun updateButtonStates() {
        if (config.showBrushSize) {
            hideBrushSettings(isEyeDropperOn || isBucketFillOn)
        }

        binding.apply {
            updateButtonColor(eraser, isEraserOn)
            updateButtonColor(eyeDropper, isEyeDropperOn)
            updateButtonColor(bucketFill, isBucketFillOn)
        }
    }

    private fun updateButtonColor(view: ImageView, enabled: Boolean) {
        val buttonColor = if (enabled) {
            getProperPrimaryColor()
        } else {
            config.canvasBackgroundColor.getContrastColor()
        }

        view.applyColorFilter(buttonColor)
    }

    private fun hideBrushSettings(hide: Boolean) {
        arrayOf(binding.strokeWidthBar, binding.strokeWidthPreview).forEach {
            it.beGoneIf(hide)
        }
    }

    private fun confirmImage() {
        when {
            isEditIntent -> {
                try {
                    val outputStream = contentResolver.openOutputStream(intentUri!!)
                    saveToOutputStream(outputStream, defaultPath.getCompressionFormat(), true)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }

            intentUri?.scheme == "content" -> {
                val outputStream = contentResolver.openOutputStream(intentUri!!)
                saveToOutputStream(outputStream, defaultPath.getCompressionFormat(), true)
            }

            else -> handlePermission(PERMISSION_WRITE_STORAGE) {
                val fileDirItem = FileDirItem(defaultPath, defaultPath.getFilenameFromPath())
                getFileOutputStream(fileDirItem, true) {
                    saveToOutputStream(it, defaultPath.getCompressionFormat(), true)
                }
            }
        }
    }

    private fun saveToOutputStream(outputStream: OutputStream?, format: Bitmap.CompressFormat, finishAfterSaving: Boolean) {
        if (outputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        val quality = if (format == Bitmap.CompressFormat.PNG) {
            100
        } else {
            70
        }

        outputStream.use {
            binding.myCanvas.getBitmap().compress(format, quality, it)
        }

        if (finishAfterSaving) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun trySaveImage() {
        if (isQPlus()) {
            SaveImageDialog(this, defaultPath, defaultFilename, defaultExtension, true) { fullPath, filename, extension ->
                val mimetype = if (extension == SVG) "svg+xml" else extension

                defaultFilename = filename
                defaultExtension = extension
                config.lastSaveExtension = extension

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "image/$mimetype"
                    putExtra(Intent.EXTRA_TITLE, "$filename.$extension")
                    addCategory(Intent.CATEGORY_OPENABLE)

                    try {
                        startActivityForResult(this, SAVE_IMAGE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        } else {
            getStoragePermission {
                saveImage()
            }
        }
    }

    private fun saveImage() {
        SaveImageDialog(this, defaultPath, defaultFilename, defaultExtension, false) { fullPath, filename, extension ->
            savedPathsHash = binding.myCanvas.getDrawingHashCode()
            saveFile(fullPath)
            defaultPath = fullPath.getParentPath()
            defaultFilename = filename
            defaultExtension = extension
            config.lastSaveFolder = defaultPath
            config.lastSaveExtension = extension
        }
    }

    private fun saveFile(path: String) {
        when (path.getFilenameExtension()) {
            SVG -> Svg.saveSvg(this, path, binding.myCanvas)
            else -> saveImageFile(path)
        }
        rescanPaths(arrayListOf(path)) {}
    }

    private fun saveImageFile(path: String) {
        val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
        getFileOutputStream(fileDirItem, true) {
            if (it != null) {
                writeToOutputStream(path, it)
                toast(R.string.file_saved)
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun writeToOutputStream(path: String, out: OutputStream) {
        out.use {
            binding.myCanvas.getBitmap().compress(path.getCompressionFormat(), 70, out)
        }
    }

    private fun shareImage() {
        getImagePath(binding.myCanvas.getBitmap()) {
            if (it != null) {
                sharePathIntent(it, BuildConfig.APPLICATION_ID)
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun getImagePath(bitmap: Bitmap, callback: (path: String?) -> Unit) {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bytes)

        val folder = File(cacheDir, FOLDER_NAME)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                callback(null)
                return
            }
        }

        val newPath = "$folder/$FILE_NAME"
        val fileDirItem = FileDirItem(newPath, FILE_NAME)
        getFileOutputStream(fileDirItem, true) {
            if (it != null) {
                try {
                    it.write(bytes.toByteArray())
                    callback(newPath)
                } catch (e: Exception) {
                } finally {
                    it.close()
                }
            } else {
                callback("")
            }
        }
    }

    private fun clearCanvas() {
        uriToLoad = null
        binding.myCanvas.clearCanvas()
        defaultExtension = PNG
        defaultPath = ""
        lastBitmapPath = ""
    }

    private fun pickColor() {
        ColorPickerDialog(this, color) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                if (isEyeDropperOn) {
                    eyeDropperClicked()
                }

                setColor(color)
            }
        }
    }

    fun setBackgroundColor(pickedColor: Int) {
        if (isEyeDropperOn) {
            eyeDropperClicked()
        }

        binding.apply {
            val contrastColor = pickedColor.getContrastColor()
            undo.applyColorFilter(contrastColor)
            eraser.applyColorFilter(contrastColor)
            redo.applyColorFilter(contrastColor)
            eyeDropper.applyColorFilter(contrastColor)
            bucketFill.applyColorFilter(contrastColor)
            if (isBlackAndWhiteTheme()) {
                strokeWidthBar.setColors(0, contrastColor, 0)
            }

            myCanvas.updateBackgroundColor(pickedColor)
            defaultExtension = PNG
            getBrushPreviewView().setStroke(getBrushStrokeSize(), contrastColor)
        }
    }

    private fun setColor(pickedColor: Int) {
        color = pickedColor
        binding.colorPicker.setFillWithStroke(color, config.canvasBackgroundColor, true)
        binding.myCanvas.setColor(color)
        isEraserOn = false
        updateEraserState()
        getBrushPreviewView().setColor(color)
    }

    private fun getBrushPreviewView() = binding.strokeWidthPreview.background as GradientDrawable

    private fun getBrushStrokeSize() = resources.getDimension(R.dimen.preview_dot_stroke_size).toInt()

    override fun toggleUndoVisibility(visible: Boolean) {
        binding.undo.beVisibleIf(visible)
    }

    override fun toggleRedoVisibility(visible: Boolean) {
        binding.redo.beVisibleIf(visible)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BITMAP_PATH, lastBitmapPath)

        if (uriToLoad != null) {
            outState.putString(URI_TO_LOAD, uriToLoad.toString())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastBitmapPath = savedInstanceState.getString(BITMAP_PATH)!!
        if (lastBitmapPath.isNotEmpty()) {
            openPath(lastBitmapPath)
        } else if (savedInstanceState.containsKey(URI_TO_LOAD)) {
            uriToLoad = Uri.parse(savedInstanceState.getString(URI_TO_LOAD))
            tryOpenUri(uriToLoad!!, intent)
        }
    }

    private fun updateBrushSize() {
        binding.apply {
            myCanvas.setBrushSize(brushSize)
            val scale = Math.max(0.03f, brushSize / 100f)
            strokeWidthPreview.scaleX = scale
            strokeWidthPreview.scaleY = scale
        }
    }

    private fun printImage() {
        val printHelper = PrintHelper(this)
        printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
        try {
            printHelper.printBitmap(getString(R.string.app_name), binding.myCanvas.getBitmap())
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(18, R.string.release_18))
            add(Release(20, R.string.release_20))
            add(Release(38, R.string.release_38))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
