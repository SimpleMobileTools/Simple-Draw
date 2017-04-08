package com.simplemobiletools.draw.dialogs

import android.graphics.Bitmap
import android.support.v7.app.AlertDialog
import android.view.WindowManager
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.draw.MyCanvas
import com.simplemobiletools.draw.R
import com.simplemobiletools.draw.Svg
import com.simplemobiletools.draw.activities.SimpleActivity
import kotlinx.android.synthetic.main.dialog_save_image.view.*
import java.io.File
import java.io.FileOutputStream

class SaveImageDialog(val activity: SimpleActivity, val curPath: String, val canvas: MyCanvas, callback: (path: String) -> Unit) {
    private val PNG = "png"
    private val SVG = "svg"
    private val SIMPLE_DRAW = "Simple Draw"

    init {
        val defaultFilename = "image_${System.currentTimeMillis() / 1000}"
        val initialFilename = if (curPath.isEmpty()) defaultFilename else curPath.getFilenameFromPath().substring(0, curPath.getFilenameFromPath().lastIndexOf("."))

        var realPath = if (curPath.isEmpty()) "${activity.internalStoragePath}/$SIMPLE_DRAW" else File(curPath).parent.trimEnd('/')
        val view = activity.layoutInflater.inflate(R.layout.dialog_save_image, null).apply {
            save_image_filename.setText(initialFilename)
            save_image_radio_group.check(if (curPath.endsWith(SVG)) R.id.save_image_radio_svg else R.id.save_image_radio_png)

            save_image_path.text = activity.humanizePath(realPath)
            save_image_path.setOnClickListener {
                FilePickerDialog(activity, realPath, false, showFAB = true) {
                    save_image_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            activity.setupDialogStuff(view, this, R.string.save_as)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val filename = view.save_image_filename.value
                if (filename.isEmpty()) {
                    activity.toast(R.string.filename_cannot_be_empty)
                    return@setOnClickListener
                }

                val extension = if (view.save_image_radio_group.checkedRadioButtonId == R.id.save_image_radio_svg) SVG else PNG
                val newFile = File(realPath, "$filename.$extension")
                if (!newFile.name.isAValidFilename()) {
                    activity.toast(R.string.filename_invalid_characters)
                    return@setOnClickListener
                }

                if (saveFile(newFile)) {
                    activity.toast(R.string.saving_ok)
                    callback(newFile.absolutePath)
                    dismiss()
                } else {
                    activity.toast(R.string.saving_error)
                }
            })
        }
    }

    private fun saveFile(file: File): Boolean {
        if (!file.parentFile.exists()) {
            if (!file.parentFile.mkdir()) {
                return false
            }
        }

        when (file.extension) {
            PNG -> {
                var out: FileOutputStream? = null
                try {
                    out = FileOutputStream(file)
                    canvas.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                    activity.scanFile(file) {}
                } catch (e: Exception) {
                    return false
                } finally {
                    out?.close()
                }
            }
            SVG -> Svg.saveSvg(file, canvas)
        }
        return true
    }
}
