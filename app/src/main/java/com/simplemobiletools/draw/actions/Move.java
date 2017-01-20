package com.simplemobiletools.draw.actions;

import android.graphics.Path;

import java.io.IOException;
import java.io.Writer;

public final class Move implements Action {

    private final float x, y;

    public Move(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void perform(Path path) {
        path.moveTo(x, y);
    }

    @Override
    public void perform(Writer writer) throws IOException {
        writer.write('M');
        writer.write(String.valueOf(x));
        writer.write(',');
        writer.write(String.valueOf(y));
    }
}
