package com.simplemobiletools.draw;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;

public class Utils {
    private final static double BRIGHTNESS_CUTOFF = 130.0;

    public static void showToast(Context cxt, int msgId) {
        Toast.makeText(cxt, cxt.getResources().getString(msgId), Toast.LENGTH_SHORT).show();
    }

    // Used to determine the best foreground color (black or white) given a background color
    public static boolean shouldUseWhite(int color) {
        float r, g, b;
        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);

        double brightness = Math.sqrt(
                r * r * .299 +
                g * g * .587 +
                b * b * .114);

        return brightness < BRIGHTNESS_CUTOFF;
    }
}
