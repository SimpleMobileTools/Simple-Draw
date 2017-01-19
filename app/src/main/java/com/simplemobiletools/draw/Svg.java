package com.simplemobiletools.draw;

import com.simplemobiletools.draw.actions.Action;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

public class Svg {

    public static boolean saveSvg(File output, MyCanvas canvas) {
        try {
            FileOutputStream out = new FileOutputStream(output);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            writeSvg(writer, canvas.getPaths(), canvas.getWidth(), canvas.getHeight());
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void writeSvg(Writer writer,
                                 Map<MyPath, PaintOptions> paths, int width, int height)
            throws IOException {
        writer.write("<svg width=\"");
        writer.write(String.valueOf(width));
        writer.write("\" height=\"");
        writer.write(String.valueOf(height));
        writer.write("\" xmlns=\"http://www.w3.org/2000/svg\">");
        for (Map.Entry<MyPath, PaintOptions> entry : paths.entrySet()) {
            writePath(writer, entry.getKey(), entry.getValue());
        }
        writer.write("</svg>");
    }

    private static void writePath(Writer writer, MyPath path, PaintOptions options)
            throws IOException {

        writer.write("<path d=\"");
        for (Action action : path.getActions()) {
            action.perform(writer);
            writer.write(' ');
        }

        writer.write("\" fill=\"none\" stroke=\"#");
        writer.write(Integer.toHexString(options.color).substring(2)); // Skip the alpha FF
        writer.write("\" stroke-width=\"");
        writer.write(String.valueOf(options.strokeWidth));
        writer.write("\"/>");
    }
}
