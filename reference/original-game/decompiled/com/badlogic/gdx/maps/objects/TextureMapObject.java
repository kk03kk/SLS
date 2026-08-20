/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.objects;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;

public class TextureMapObject
extends MapObject {
    private float x = 0.0f;
    private float y = 0.0f;
    private float originX = 0.0f;
    private float originY = 0.0f;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float rotation = 0.0f;
    private TextureRegion textureRegion = null;

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getOriginX() {
        return this.originX;
    }

    public void setOriginX(float x) {
        this.originX = x;
    }

    public float getOriginY() {
        return this.originY;
    }

    public void setOriginY(float y) {
        this.originY = y;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public void setScaleX(float x) {
        this.scaleX = x;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public void setScaleY(float y) {
        this.scaleY = y;
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public TextureRegion getTextureRegion() {
        return this.textureRegion;
    }

    public void setTextureRegion(TextureRegion region) {
        this.textureRegion = region;
    }

    public TextureMapObject() {
        this(null);
    }

    public TextureMapObject(TextureRegion textureRegion) {
        this.textureRegion = textureRegion;
    }
}

