package com.simplemobiletools.draw.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.SeekBar
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.draw.BuildConfig
import com.simplemobiletools.draw.MyCanvas
import com.simplemobiletools.draw.R
import com.simplemobiletools.draw.Svg
import com.simplemobiletools.draw.dialogs.SaveImageDialog
import com.simplemobiletools.draw.extensions.config
import com.simplemobiletools.draw.helpers.JPG
import com.simplemobiletools.draw.helpers.PNG
import com.simplemobiletools.draw.helpers.SVG
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : SimpleActivity(), MyCanvas.PathsChangedListener {
    private val FOLDER_NAME = "images"
    private val FILE_NAME = "simple-draw.png"

    private var curPath = ""
    private var color = 0
    private var strokeWidth = 0f
    private var suggestedFileExtension = PNG
    private var isEraserOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        storeStoragePaths()
        my_canvas.setListener(this)
        stroke_width_bar.setOnSeekBarChangeListener(onStrokeWidthBarChangeListener)

        setBackgroundColor(config.canvasBackgroundColor)
        setColor(config.brushColor)

        strokeWidth = config.brushSize
        my_canvas.setStrokeWidth(strokeWidth)
        stroke_width_bar.progress = strokeWidth.toInt()

        color_picker.setOnClickListener { pickColor() }
        undo.setOnClickListener { my_canvas.undo() }
        eraser.setOnClickListener { eraserClicked() }

        checkIntents()
        checkWhatsNewDialog()
    }

    override fun onResume() {
        super.onResume()
        val isStrokeWidthBarEnabled = config.showBrushSize
        stroke_width_bar.beVisibleIf(isStrokeWidthBarEnabled)
        my_canvas.setIsStrokeWidthBarEnabled(isStrokeWidthBarEnabled)
        updateTextColors(main_holder)
    }

    override fun onPause() {
        super.onPause()
        config.brushColor = color
        config.brushSize = strokeWidth
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> trySaveImage()
            R.id.menu_share -> shareImage()
            R.id.clear -> clearCanvas()
            R.id.open_file -> tryOpenFile()
            R.id.change_background -> changeBackgroundClicked()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN, BuildConfig.VERSION_NAME)
    }

    private fun tryOpenFile() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                openFile()
            } else {
                toast(R.string.no_storage_permissions)
            }
        }
    }

    private fun openFile() {
        FilePickerDialog(this, curPath) {
            openPath(it)
        }
    }

    private fun checkIntents() {
        if (intent?.action == Intent.ACTION_SEND && intent.type.startsWith("image/")) {
            getStoragePermission {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                tryOpenUri(uri)
            }
        }

        if (intent?.action == Intent.ACTION_SEND_MULTIPLE && intent.type.startsWith("image/")) {
            getStoragePermission {
                val imageUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                imageUris.any { tryOpenUri(it) }
            }
        }

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            getStoragePermission {
                val path = intent.data.path
                openPath(path)
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

    private fun tryOpenUri(uri: Uri) = when {
        uri.scheme == "file" -> openPath(uri.path)
        uri.scheme == "content" -> openUri(uri, intent)
        else -> false
    }

    private fun openPath(path: String) = when {
        path.endsWith(".svg") -> {
            my_canvas.mBackgroundBitmap = null
            Svg.loadSvg(this, File(path), my_canvas)
            suggestedFileExtension = SVG
            true
        }
        File(path).isImageSlow() -> {
            my_canvas.drawBitmap(this, path)
            suggestedFileExtension = JPG
            true
        }
        else -> {
            toast(R.string.invalid_file_format)
            false
        }
    }

    private fun openUri(uri: Uri, intent: Intent): Boolean {
        val mime = MimeTypeMap.getSingleton()
        val type = mime.getExtensionFromMimeType(contentResolver.getType(uri)) ?: intent.type
        return when (type) {
            "svg", "image/svg+xml" -> {
                my_canvas.mBackgroundBitmap = null
                Svg.loadSvg(this, uri, my_canvas)
                suggestedFileExtension = SVG
                true
            }
            "jpg", "jpeg", "png" -> {
                my_canvas.drawBitmap(this, uri)
                suggestedFileExtension = JPG
                true
            }
            else -> {
                toast(R.string.invalid_file_format)
                false
            }
        }
    }

    private fun eraserClicked() {
        isEraserOn = !isEraserOn
        updateEraserState()
    }

    private fun updateEraserState() {
        eraser.setImageDrawable(resources.getDrawable(if (isEraserOn) R.drawable.ic_eraser_on else R.drawable.ic_eraser_off))
        my_canvas.toggleEraser(isEraserOn)
    }

    private fun changeBackgroundClicked() {
        val oldColor = (my_canvas.background as ColorDrawable).color
        ColorPickerDialog(this, oldColor) {
            setBackgroundColor(it)
            config.canvasBackgroundColor = it
        }
    }

    private fun trySaveImage() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                saveImage()
            } else {
                toast(R.string.no_storage_permissions)
            }
        }
    }

    private fun saveImage() {
        SaveImageDialog(this, suggestedFileExtension, curPath, my_canvas) {
            curPath = it
        }
    }

    private fun shareImage() {
        val shareTitle = resources.getString(R.string.share_via)
        val uri = getImageUri(my_canvas.getBitmap()) ?: return

        Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(uri, contentResolver.getType(uri))
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/*"
            startActivity(Intent.createChooser(this, shareTitle))
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bytes)

        val folder = File(cacheDir, FOLDER_NAME)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                return null
            }
        }

        val file = File(folder, FILE_NAME)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(bytes.toByteArray())
        } catch (e: Exception) {
        } finally {
            fileOutputStream?.close()
        }

        return FileProvider.getUriForFile(this, "com.simplemobiletools.draw.fileprovider", file)
    }

    private fun clearCanvas() {
        my_canvas.clearCanvas()
        suggestedFileExtension = PNG
        curPath = ""
    }

    private fun pickColor() {
        ColorPickerDialog(this, color) {
            setColor(it)
        }
    }

    fun setBackgroundColor(pickedColor: Int) {
        undo.setColorFilter(pickedColor.getContrastColor(), PorterDuff.Mode.SRC_IN)
        eraser.setColorFilter(pickedColor.getContrastColor(), PorterDuff.Mode.SRC_IN)
        my_canvas.updateBackgroundColor(pickedColor)
        suggestedFileExtension = PNG
    }

    private fun setColor(pickedColor: Int) {
        color = pickedColor
        color_picker.setBackgroundColor(color)
        my_canvas.setColor(color)
        isEraserOn = false
        updateEraserState()
    }

    override fun pathsChanged(cnt: Int) {
        undo.beVisibleIf(cnt > 0)
    }

    private var onStrokeWidthBarChangeListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            my_canvas.setStrokeWidth(progress.toFloat())
            strokeWidth = progress.toFloat()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(18, R.string.release_18))
            add(Release(20, R.string.release_20))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
