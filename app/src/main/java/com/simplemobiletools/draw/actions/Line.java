package com.simplemobiletools.draw.actions;

import android.graphics.Path;

import java.io.IOException;
import java.io.Writer;
import java.security.InvalidParameterException;

public final class Line implements Action {

    public final float x, y;

    public Line(String data) {
        if (data.startsWith("L"))
            throw new InvalidParameterException("The Line data should start with 'L'.");

        try {
            String[] xy = data.substring(1).split(",");
            x = Float.parseFloat(xy[0].trim());
            y = Float.parseFloat(xy[1].trim());
        } catch (Exception ignored) {
            throw new InvalidParameterException("Error parsing the given Line data.");
        }
    }

    public Line(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void perform(Path path) {
        path.lineTo(x, y);
    }

    @Override
    public void perform(Writer writer) throws IOException {
        writer.write("L");
        writer.write(String.valueOf(x));
        writer.write(",");
        writer.write(String.valueOf(y));
    }
}
