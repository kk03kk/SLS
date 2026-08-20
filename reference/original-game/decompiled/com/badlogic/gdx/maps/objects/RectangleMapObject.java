/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Rectangle;

public class RectangleMapObject
extends MapObject {
    private Rectangle rectangle;

    public Rectangle getRectangle() {
        return this.rectangle;
    }

    public RectangleMapObject() {
        this(0.0f, 0.0f, 1.0f, 1.0f);
    }

    public RectangleMapObject(float x, float y, float width, float height) {
        this.rectangle = new Rectangle(x, y, width, height);
    }
}

