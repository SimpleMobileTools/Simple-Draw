package com.simplemobiletools.draw.actions;

import android.graphics.Path;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

public interface Action extends Serializable {
    void perform(Path path);
    void perform(Writer writer) throws IOException;
}
