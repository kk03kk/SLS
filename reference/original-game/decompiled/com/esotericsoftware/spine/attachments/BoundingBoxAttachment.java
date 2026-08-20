/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.VertexAttachment;

public class BoundingBoxAttachment
extends VertexAttachment {
    final Color color = new Color(0.38f, 0.94f, 0.0f, 1.0f);

    public BoundingBoxAttachment(String name) {
        super(name);
    }

    @Override
    public void computeWorldVertices(Slot slot, float[] worldVertices) {
        this.computeWorldVertices(slot, 0, this.worldVerticesLength, worldVertices, 0);
    }

    public Color getColor() {
        return this.color;
    }
}

