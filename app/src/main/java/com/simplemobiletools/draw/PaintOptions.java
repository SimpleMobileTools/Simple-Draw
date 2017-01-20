package com.simplemobiletools.draw;

import android.graphics.Color;

class PaintOptions {
    int color = Color.BLACK;
    float strokeWidth = 5f;

    PaintOptions() {

    }

    PaintOptions(int color, float strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }
}
