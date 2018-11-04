package com.simplemobiletools.draw.pro.models

import android.os.Parcel
import android.os.Parcelable
import android.view.View
import java.util.*

internal class MyParcelable : View.BaseSavedState {
    var paths = LinkedHashMap<MyPath, PaintOptions>()

    constructor(superState: Parcelable) : super(superState)

    constructor(parcel: Parcel) : super(parcel) {
        val size = parcel.readInt()
        for (i in 0 until size) {
            val key = parcel.readSerializable() as MyPath
            val paintOptions = PaintOptions(parcel.readInt(), parcel.readFloat(), parcel.readInt() == 1)
            paths[key] = paintOptions
        }
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeInt(paths.size)
        for ((path, paintOptions) in paths) {
            out.writeSerializable(path)
            out.writeInt(paintOptions.color)
            out.writeFloat(paintOptions.strokeWidth)
            out.writeInt(if (paintOptions.isEraser) 1 else 0)
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MyParcelable> = object : Parcelable.Creator<MyParcelable> {
            override fun createFromParcel(source: Parcel) = MyParcelable(source)

            override fun newArray(size: Int) = arrayOf<MyParcelable>()
        }
    }
}
