package com.simplemobiletools.draw.pro.models

import android.os.Parcel
import android.os.Parcelable
import android.view.View

internal class MyParcelable : View.BaseSavedState {
    var operations = ArrayList<CanvasOp>()

    constructor(superState: Parcelable) : super(superState)

    constructor(parcel: Parcel) : super(parcel) {
        val size = parcel.readInt()
        for (i in 0 until size) {
            val serializable = parcel.readSerializable()
            if (serializable is MyPath) {
                val paintOptions = PaintOptions(parcel.readInt(), parcel.readFloat(), parcel.readInt() == 1)
                val operation = CanvasOp.PathOp(serializable, paintOptions)
                operations.add(operation)
            }
        }
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeInt(operations.size)
        for (operation in operations) {
            if (operation is CanvasOp.PathOp) {
                val path = operation.path
                val paintOptions = operation.paintOptions
                out.writeSerializable(path)
                out.writeInt(paintOptions.color)
                out.writeFloat(paintOptions.strokeWidth)
                out.writeInt(if (paintOptions.isEraser) 1 else 0)
            }
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
