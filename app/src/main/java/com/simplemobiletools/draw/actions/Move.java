package com.simplemobiletools.draw.actions;

import android.graphics.Path;

import java.io.IOException;
import java.io.Writer;
import java.security.InvalidParameterException;

public final class Move implements Action {

    public final float x, y;

    public Move(String data) throws InvalidParameterException {
        if (data.startsWith("M"))
            throw new InvalidParameterException("The Move data should start with 'M'.");

        try {
            String[] xy = data.substring(1).split(",");
            x = Float.parseFloat(xy[0].trim());
            y = Float.parseFloat(xy[1].trim());
        } catch (Exception ignored) {
            throw new InvalidParameterException("Error parsing the given Move data.");
        }
    }

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
