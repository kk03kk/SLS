/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.VertexAttachment;

public class PathAttachment
extends VertexAttachment {
    float[] lengths;
    boolean closed;
    boolean constantSpeed;
    final Color color = new Color(1.0f, 0.5f, 0.0f, 1.0f);

    public PathAttachment(String name) {
        super(name);
    }

    @Override
    public void computeWorldVertices(Slot slot, float[] worldVertices) {
        super.computeWorldVertices(slot, worldVertices);
    }

    @Override
    public void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset) {
        super.computeWorldVertices(slot, start, count, worldVertices, offset);
    }

    public boolean getClosed() {
        return this.closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean getConstantSpeed() {
        return this.constantSpeed;
    }

    public void setConstantSpeed(boolean constantSpeed) {
        this.constantSpeed = constantSpeed;
    }

    public float[] getLengths() {
        return this.lengths;
    }

    public void setLengths(float[] lengths) {
        this.lengths = lengths;
    }

    public Color getColor() {
        return this.color;
    }
}

