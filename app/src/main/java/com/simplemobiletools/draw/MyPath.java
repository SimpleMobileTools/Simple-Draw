package com.simplemobiletools.draw;

import android.graphics.Path;

import com.simplemobiletools.draw.actions.Action;
import com.simplemobiletools.draw.actions.Line;
import com.simplemobiletools.draw.actions.Move;
import com.simplemobiletools.draw.actions.Quad;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

// https://stackoverflow.com/a/8127953
class MyPath extends Path implements Serializable {

    private List<Action> actions = new LinkedList<>();

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        for (Action action : actions) {
            action.perform(this);
        }
    }

    @Override
    public void reset() {
        actions.clear();
        super.reset();
    }

    @Override
    public void moveTo(float x, float y) {
        actions.add(new Move(x, y));
        super.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y) {
        actions.add(new Line(x, y));
        super.lineTo(x, y);
    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {
        actions.add(new Quad(x1, y1, x2, y2));
        super.quadTo(x1, y1, x2, y2);
    }

    List<Action> getActions() {
        return actions;
    }
}
