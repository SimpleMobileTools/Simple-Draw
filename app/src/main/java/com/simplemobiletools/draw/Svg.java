package com.simplemobiletools.draw;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import com.simplemobiletools.draw.actions.Action;

import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;

public class Svg {

    //region Saving

    public static void saveSvg(File output, MyCanvas canvas) throws Exception {
        int backgroundColor = ((ColorDrawable) canvas.getBackground()).getColor();

        FileOutputStream out = new FileOutputStream(output);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        writeSvg(writer, backgroundColor, canvas.getPaths(), canvas.getWidth(), canvas.getHeight());
        writer.close();
    }

    private static void writeSvg(Writer writer, int backgroundColor, Map<MyPath, PaintOptions> paths, int width, int height) throws IOException {
        writer.write("<svg width=\"");
        writer.write(String.valueOf(width));
        writer.write("\" height=\"");
        writer.write(String.valueOf(height));
        writer.write("\" xmlns=\"http://www.w3.org/2000/svg\">");

        // background rect
        writer.write("<rect width=\"");
        writer.write(String.valueOf(width));
        writer.write("\" height=\"");
        writer.write(String.valueOf(height));
        writer.write("\" fill=\"#");
        writer.write(Integer.toHexString(backgroundColor).substring(2)); // Skip the alpha FF
        writer.write("\"/>");

        for (Map.Entry<MyPath, PaintOptions> entry : paths.entrySet()) {
            writePath(writer, entry.getKey(), entry.getValue());
        }
        writer.write("</svg>");
    }

    private static void writePath(Writer writer, MyPath path, PaintOptions options) throws IOException {
        writer.write("<path d=\"");
        for (Action action : path.getActions()) {
            action.perform(writer);
            writer.write(' ');
        }

        writer.write("\" fill=\"none\" stroke=\"#");
        writer.write(Integer.toHexString(options.color).substring(2)); // Skip the alpha FF
        writer.write("\" stroke-width=\"");
        writer.write(String.valueOf(options.strokeWidth));
        writer.write("\" stroke-linecap=\"round\"/>");
    }

    //endregion

    //region Loading

    public static void loadSvg(File file, MyCanvas canvas) throws Exception {
        SSvg svg = parseSvg(file);

        canvas.clearCanvas();
        canvas.setBackgroundColor(svg.background.color);

        for (SPath sp : svg.paths) {
            MyPath path = new MyPath();
            path.readObject(sp.data);
            PaintOptions options = new PaintOptions(sp.color, sp.strokeWidth);

            canvas.addPath(path, options);
        }
    }

    public static SSvg parseSvg(File file) throws Exception {
        InputStream is = null;
        final SSvg svg = new SSvg();
        try {
            is = new FileInputStream(file);

            // Actual parsing (http://stackoverflow.com/a/4828765)
            final String ns = "http://www.w3.org/2000/svg";
            RootElement root = new RootElement(ns, "svg");
            Element rectElement = root.getChild(ns, "rect");
            final Element pathElement = root.getChild(ns, "path");

            root.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    int width = Integer.parseInt(attributes.getValue("width"));
                    int height = Integer.parseInt(attributes.getValue("height"));
                    svg.setSize(width, height);
                }
            });

            rectElement.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    int width = Integer.parseInt(attributes.getValue("width"));
                    int height = Integer.parseInt(attributes.getValue("height"));
                    int color = Color.parseColor(attributes.getValue("fill"));
                    if (svg.background != null)
                        throw new UnsupportedOperationException("Unsupported SVG, should only have one <rect>.");

                    svg.background = new SRect(width, height, color);
                }
            });

            pathElement.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    String d = attributes.getValue("d");
                    int color = Color.parseColor(attributes.getValue("stroke"));
                    float width = Float.parseFloat(attributes.getValue("stroke-width"));
                    svg.paths.add(new SPath(d, color, width));
                }
            });

            // Once the parsing is set up, parse this InputStream
            Xml.parse(is, Xml.Encoding.UTF_8, root.getContentHandler());
        } finally {
            if (is != null)
                is.close();
        }
        return svg;
    }

    //region Svg serializable classes

    private static class SSvg implements Serializable {
        int width;
        int height;
        SRect background;
        final ArrayList<SPath> paths;

        SSvg() {
            paths = new ArrayList<>();
        }

        void setSize(int w, int h) {
            width = w;
            height = h;
        }
    }

    private static class SRect implements Serializable {
        final int width;
        final int height;
        final int color;

        SRect(int w, int h, int c) {
            width = w;
            height = h;
            color = c;
        }
    }

    private static class SPath implements Serializable {
        String data;
        int color;
        float strokeWidth;

        SPath(String d, int c, float w) {
            data = d;
            color = c;
            strokeWidth = w;
        }
    }

    //endregion

    //endregion
}
