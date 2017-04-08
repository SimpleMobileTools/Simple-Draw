package com.simplemobiletools.draw.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.hasWriteStoragePermission
import com.simplemobiletools.commons.extensions.storeStoragePaths
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.draw.BuildConfig
import com.simplemobiletools.draw.MyCanvas
import com.simplemobiletools.draw.R
import com.simplemobiletools.draw.dialogs.SaveImageDialog
import com.simplemobiletools.draw.extensions.config
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : SimpleActivity(), MyCanvas.PathsChangedListener {
    private val FOLDER_NAME = "images"
    private val FILE_NAME = "simple-draw.png"
    private val STORAGE_PERMISSION = 1

    private var curPath = ""
    private var color = 0
    private var strokeWidth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        my_canvas.setListener(this)
        stroke_width_bar.setOnSeekBarChangeListener(onStrokeWidthBarChangeListener)

        setBackgroundColor(config.canvasBackgroundColor)
        setColor(config.brushColor)

        strokeWidth = config.brushSize
        my_canvas.setStrokeWidth(strokeWidth)
        stroke_width_bar.progress = strokeWidth.toInt()

        color_picker.setOnClickListener { pickColor() }
        undo.setOnClickListener { my_canvas.undo() }
        storeStoragePaths()
    }

    override fun onResume() {
        super.onResume()
        val isStrokeWidthBarEnabled = config.showBrushSize
        stroke_width_bar.beVisibleIf(isStrokeWidthBarEnabled)
        my_canvas.setIsStrokeWidthBarEnabled(isStrokeWidthBarEnabled)
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
            R.id.clear -> my_canvas.clearCanvas()
            R.id.change_background -> changeBackgroundClicked()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage()
            } else {
                toast(R.string.no_permissions)
            }
        }
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN, BuildConfig.VERSION_NAME)
    }

    private fun changeBackgroundClicked() {
        val oldColor = (my_canvas.background as ColorDrawable).color
        ColorPickerDialog(this, oldColor) {
            setBackgroundColor(it)
            config.canvasBackgroundColor = it
        }
    }

    private fun trySaveImage() {
        if (!hasWriteStoragePermission()) {
            saveImage()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    private fun saveImage() {
        SaveImageDialog(this, curPath, my_canvas) {
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
            if (!folder.mkdir())
                return null
        }

        val file = File(folder, FILE_NAME)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(bytes.toByteArray())
        } catch (e: Exception) {
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: Exception) {
            }
        }

        return FileProvider.getUriForFile(this, "com.simplemobiletools.draw.fileprovider", file)
    }

    fun pickColor() {
        ColorPickerDialog(this, color) {
            setColor(it)
        }
    }

    private fun setBackgroundColor(pickedColor: Int) {
        undo.setImageResource(R.drawable.ic_undo)
        my_canvas.setBackgroundColor(pickedColor)
    }

    private fun setColor(pickedColor: Int) {
        color = pickedColor
        color_picker.setBackgroundColor(color)
        my_canvas.setColor(color)
    }

    override fun pathsChanged(cnt: Int) {
        undo.beVisibleIf(cnt > 0)
    }

    internal var onStrokeWidthBarChangeListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            my_canvas.setStrokeWidth(progress.toFloat())
            strokeWidth = progress.toFloat()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
}
