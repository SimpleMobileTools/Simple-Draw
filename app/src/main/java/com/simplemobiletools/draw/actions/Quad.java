package com.simplemobiletools.draw.actions;

import android.graphics.Path;

import java.io.IOException;
import java.io.Writer;
import java.security.InvalidParameterException;

public final class Quad implements Action {

    public final float x1, y1, x2, y2;

    public Quad(String data) {
        if (data.startsWith("Q"))
            throw new InvalidParameterException("The Quad data should start with 'Q'.");

        try {
            String[] parts = data.split("\\s+");
            String[] xy1 = parts[0].substring(1).split(",");
            String[] xy2 = parts[1].split(","); // No need to skip the 'Q' here

            x1 = Float.parseFloat(xy1[0].trim());
            y1 = Float.parseFloat(xy1[1].trim());
            x2 = Float.parseFloat(xy2[0].trim());
            y2 = Float.parseFloat(xy2[1].trim());
        } catch (Exception ignored) {
            throw new InvalidParameterException("Error parsing the given Quad data.");
        }
    }

    public Quad(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public void perform(Path path) {
        path.quadTo(x1, y1, x2, y2);
    }

    @Override
    public void perform(Writer writer) throws IOException {
        writer.write('Q');
        writer.write(String.valueOf(x1));
        writer.write(',');
        writer.write(String.valueOf(y1));
        writer.write(' ');
        writer.write(String.valueOf(x2));
        writer.write(',');
        writer.write(String.valueOf(y2));
    }
}
