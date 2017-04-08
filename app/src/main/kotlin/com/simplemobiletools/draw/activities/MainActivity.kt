package com.simplemobiletools.draw.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.simplemobiletools.commons.activities.AboutActivity
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.draw.MyCanvas
import com.simplemobiletools.draw.R
import com.simplemobiletools.draw.Svg
import com.simplemobiletools.draw.helpers.Config
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : SimpleActivity(), MyCanvas.PathsChangedListener {

    @BindView(R.id.my_canvas) internal var mMyCanvas: MyCanvas? = null
    @BindView(R.id.undo) internal var mUndoBtn: View? = null
    @BindView(R.id.color_picker) internal var mColorPicker: View? = null
    @BindView(R.id.stroke_width_bar) internal var mStrokeWidthBar: SeekBar? = null

    private var curFileName: String? = null
    private var curExtensionId = 0

    private var color = 0
    private var strokeWidth = 0f

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val FOLDER_NAME = "images"
        private val FILE_NAME = "simple-draw.png"
        private val SAVE_FOLDER_NAME = "Simple Draw"
        private val STORAGE_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        mMyCanvas!!.setListener(this)
        mStrokeWidthBar!!.setOnSeekBarChangeListener(onStrokeWidthBarChangeListener)

        setBackgroundColor(Config.newInstance(this).canvasBackgroundColor)
        setColor(Config.newInstance(this).brushColor)

        strokeWidth = Config.newInstance(this).brushSize
        mMyCanvas!!.setStrokeWidth(strokeWidth)
        mStrokeWidthBar!!.progress = strokeWidth.toInt()
    }

    override fun onResume() {
        super.onResume()
        val isStrokeWidthBarEnabled = Config.newInstance(this).showBrushSize
        mStrokeWidthBar!!.visibility = if (isStrokeWidthBarEnabled) View.VISIBLE else View.GONE
        mMyCanvas!!.setIsStrokeWidthBarEnabled(isStrokeWidthBarEnabled)
    }

    override fun onPause() {
        super.onPause()
        Config.newInstance(this).brushColor = color
        Config.newInstance(this).brushSize = strokeWidth
    }

    override fun onDestroy() {
        super.onDestroy()
        Config.newInstance(applicationContext).isFirstRun = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                saveImage()
                return true
            }
            R.id.menu_share -> {
                shareImage()
                return true
            }
            R.id.settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                return true
            }
            R.id.clear -> {
                mMyCanvas!!.clearCanvas()
                return true
            }
            R.id.change_background -> {
                val oldColor = (mMyCanvas!!.background as ColorDrawable).color
                val dialog = AmbilWarnaDialog(this, oldColor,
                        object : AmbilWarnaDialog.OnAmbilWarnaListener {
                            override fun onCancel(dialog: AmbilWarnaDialog) {}

                            override fun onOk(dialog: AmbilWarnaDialog, pickedColor: Int) {
                                setBackgroundColor(pickedColor)
                                Config.newInstance(applicationContext).canvasBackgroundColor = pickedColor
                            }
                        })

                dialog.show()
                return true
            }
            R.id.about -> {
                startActivity(Intent(applicationContext, AboutActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage()
            } else {
                Toast.makeText(this, resources.getString(R.string.no_permissions), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION)
            return
        }

        val saveFileView = layoutInflater.inflate(R.layout.save_file, null)

        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.save_file))

        val fileNameET = saveFileView.findViewById(R.id.file_name) as EditText
        fileNameET.setText(curFileName)

        val fileExtensionRG = saveFileView.findViewById(R.id.extension_radio_group) as RadioGroup
        if (curExtensionId != 0) {
            fileExtensionRG.check(curExtensionId)
        }
        builder.setView(saveFileView)

        builder.setPositiveButton(R.string.ok, null)
        builder.setNegativeButton(R.string.cancel, null)

        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val fileName = fileNameET.text.toString().trim { it <= ' ' }
            if (!fileName.isEmpty()) {
                val extension: String
                when (fileExtensionRG.checkedRadioButtonId) {
                    R.id.extension_radio_svg -> extension = ".svg"
                    else -> extension = ".png"
                }

                if (saveFile(fileName, extension)) {
                    curFileName = fileName
                    curExtensionId = fileExtensionRG.checkedRadioButtonId

                    toast(R.string.saving_ok)
                    alertDialog.dismiss()
                } else {
                    toast(R.string.saving_error)
                }
            } else {
                toast(R.string.enter_file_name)
            }
        }
    }

    private fun saveFile(fileName: String, extension: String): Boolean {
        val path = Environment.getExternalStorageDirectory().toString()
        val directory = File(path, SAVE_FOLDER_NAME)
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                return false
            }
        }

        val file = File(directory, fileName + extension)
        when (extension) {
            ".png" -> {
                val bitmap = mMyCanvas!!.bitmap
                var out: FileOutputStream? = null
                try {
                    out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), null, null)
                } catch (e: Exception) {
                    Log.e(TAG, "MainActivity SaveFile (.png) " + e.message)
                    return false
                } finally {
                    try {
                        if (out != null) {
                            out.close()
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "MainActivity SaveFile (.png) 2 " + e.message)
                    }

                }
            }
            ".svg" -> {
                try {
                    Svg.saveSvg(file, mMyCanvas!!)
                } catch (e: Exception) {
                    Log.e(TAG, "MainActivity SaveFile (.svg) " + e.message)
                    return false
                }

            }
            else -> return false
        }

        return true
    }

    private fun shareImage() {
        val shareTitle = resources.getString(R.string.share_via)
        val bitmap = mMyCanvas!!.bitmap
        val sendIntent = Intent()
        val uri = getImageUri(bitmap) ?: return

        sendIntent.action = Intent.ACTION_SEND
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sendIntent.setDataAndType(uri, contentResolver.getType(uri))
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sendIntent.type = "image/*"
        startActivity(Intent.createChooser(sendIntent, shareTitle))
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
            Log.e(TAG, "getImageUri 1 " + e.message)
        } finally {
            try {
                if (fileOutputStream != null)
                    fileOutputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "getImageUri 2 " + e.message)
            }

        }

        return FileProvider.getUriForFile(this, "com.simplemobiletools.draw.fileprovider", file)
    }

    @OnClick(R.id.undo)
    fun undo() {
        mMyCanvas!!.undo()
    }

    @OnClick(R.id.color_picker)
    fun pickColor() {
        val dialog = AmbilWarnaDialog(this, color, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {}

            override fun onOk(dialog: AmbilWarnaDialog, pickedColor: Int) {
                setColor(pickedColor)
            }
        })

        dialog.show()
    }

    private fun setBackgroundColor(pickedColor: Int) {
        (mUndoBtn as ImageView).setImageResource(R.drawable.ic_undo)
        mMyCanvas!!.setBackgroundColor(pickedColor)
    }

    private fun setColor(pickedColor: Int) {
        color = pickedColor
        mColorPicker!!.setBackgroundColor(color)
        mMyCanvas!!.setColor(color)
    }

    override fun pathsChanged(cnt: Int) {
        mUndoBtn!!.visibility = if (cnt > 0) View.VISIBLE else View.GONE
    }

    internal var onStrokeWidthBarChangeListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mMyCanvas!!.setStrokeWidth(progress.toFloat())
            strokeWidth = progress.toFloat()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
}
