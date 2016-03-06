package com.simplemobiletools.draw;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void showToast(Context cxt, int msgId) {
        Toast.makeText(cxt, cxt.getResources().getString(msgId), Toast.LENGTH_SHORT).show();
    }
}
