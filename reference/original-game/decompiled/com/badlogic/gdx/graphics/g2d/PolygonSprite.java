/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.NumberUtils;

public class PolygonSprite {
    PolygonRegion region;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float rotation;
    private float originX;
    private float originY;
    private float[] vertices;
    private boolean dirty;
    private Rectangle bounds = new Rectangle();
    private final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    public PolygonSprite(PolygonRegion region) {
        this.setRegion(region);
        this.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.setSize(region.region.regionWidth, region.region.regionHeight);
        this.setOrigin(this.width / 2.0f, this.height / 2.0f);
    }

    public PolygonSprite(PolygonSprite sprite) {
        this.set(sprite);
    }

    public void set(PolygonSprite sprite) {
        if (sprite == null) {
            throw new IllegalArgumentException("sprite cannot be null.");
        }
        this.setRegion(sprite.region);
        this.x = sprite.x;
        this.y = sprite.y;
        this.width = sprite.width;
        this.height = sprite.height;
        this.originX = sprite.originX;
        this.originY = sprite.originY;
        this.rotation = sprite.rotation;
        this.scaleX = sprite.scaleX;
        this.scaleY = sprite.scaleY;
        this.color.set(sprite.color);
        this.dirty = sprite.dirty;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.dirty = true;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
        this.dirty = true;
    }

    public void setPosition(float x, float y) {
        this.translate(x - this.x, y - this.y);
    }

    public void setX(float x) {
        this.translateX(x - this.x);
    }

    public void setY(float y) {
        this.translateY(y - this.y);
    }

    public void translateX(float xAmount) {
        this.x += xAmount;
        if (this.dirty) {
            return;
        }
        float[] vertices = this.vertices;
        for (int i = 0; i < vertices.length; i += 5) {
            int n = i;
            vertices[n] = vertices[n] + xAmount;
        }
    }

    public void translateY(float yAmount) {
        this.y += yAmount;
        if (this.dirty) {
            return;
        }
        float[] vertices = this.vertices;
        for (int i = 1; i < vertices.length; i += 5) {
            int n = i;
            vertices[n] = vertices[n] + yAmount;
        }
    }

    public void translate(float xAmount, float yAmount) {
        this.x += xAmount;
        this.y += yAmount;
        if (this.dirty) {
            return;
        }
        float[] vertices = this.vertices;
        for (int i = 0; i < vertices.length; i += 5) {
            int n = i;
            vertices[n] = vertices[n] + xAmount;
            int n2 = i + 1;
            vertices[n2] = vertices[n2] + yAmount;
        }
    }

    public void setColor(Color tint) {
        this.color.set(tint);
        float color = tint.toFloatBits();
        float[] vertices = this.vertices;
        for (int i = 2; i < vertices.length; i += 5) {
            vertices[i] = color;
        }
    }

    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
        int intBits = (int)(255.0f * a) << 24 | (int)(255.0f * b) << 16 | (int)(255.0f * g) << 8 | (int)(255.0f * r);
        float color = NumberUtils.intToFloatColor(intBits);
        float[] vertices = this.vertices;
        for (int i = 2; i < vertices.length; i += 5) {
            vertices[i] = color;
        }
    }

    public void setOrigin(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
        this.dirty = true;
    }

    public void setRotation(float degrees) {
        this.rotation = degrees;
        this.dirty = true;
    }

    public void rotate(float degrees) {
        this.rotation += degrees;
        this.dirty = true;
    }

    public void setScale(float scaleXY) {
        this.scaleX = scaleXY;
        this.scaleY = scaleXY;
        this.dirty = true;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.dirty = true;
    }

    public void scale(float amount) {
        this.scaleX += amount;
        this.scaleY += amount;
        this.dirty = true;
    }

    public float[] getVertices() {
        if (!this.dirty) {
            return this.vertices;
        }
        this.dirty = false;
        float originX = this.originX;
        float originY = this.originY;
        float scaleX = this.scaleX;
        float scaleY = this.scaleY;
        PolygonRegion region = this.region;
        float[] vertices = this.vertices;
        float[] regionVertices = region.vertices;
        float worldOriginX = this.x + originX;
        float worldOriginY = this.y + originY;
        float sX = this.width / (float)region.region.getRegionWidth();
        float sY = this.height / (float)region.region.getRegionHeight();
        float cos = MathUtils.cosDeg(this.rotation);
        float sin = MathUtils.sinDeg(this.rotation);
        int i = 0;
        int v = 0;
        int n = regionVertices.length;
        while (i < n) {
            float fx = (regionVertices[i] * sX - originX) * scaleX;
            float fy = (regionVertices[i + 1] * sY - originY) * scaleY;
            vertices[v] = cos * fx - sin * fy + worldOriginX;
            vertices[v + 1] = sin * fx + cos * fy + worldOriginY;
            i += 2;
            v += 5;
        }
        return vertices;
    }

    public Rectangle getBoundingRectangle() {
        float[] vertices = this.getVertices();
        float minx = vertices[0];
        float miny = vertices[1];
        float maxx = vertices[0];
        float maxy = vertices[1];
        for (int i = 5; i < vertices.length; i += 5) {
            float x = vertices[i];
            float y = vertices[i + 1];
            minx = minx > x ? x : minx;
            maxx = maxx < x ? x : maxx;
            miny = miny > y ? y : miny;
            maxy = maxy < y ? y : maxy;
        }
        this.bounds.x = minx;
        this.bounds.y = miny;
        this.bounds.width = maxx - minx;
        this.bounds.height = maxy - miny;
        return this.bounds;
    }

    public void draw(PolygonSpriteBatch spriteBatch) {
        PolygonRegion region = this.region;
        spriteBatch.draw(region.region.texture, this.getVertices(), 0, this.vertices.length, region.triangles, 0, region.triangles.length);
    }

    public void draw(PolygonSpriteBatch spriteBatch, float alphaModulation) {
        Color color = this.getColor();
        float oldAlpha = color.a;
        color.a *= alphaModulation;
        this.setColor(color);
        this.draw(spriteBatch);
        color.a = oldAlpha;
        this.setColor(color);
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public float getOriginX() {
        return this.originX;
    }

    public float getOriginY() {
        return this.originY;
    }

    public float getRotation() {
        return this.rotation;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public Color getColor() {
        return this.color;
    }

    public Color getVertexColor() {
        int intBits = NumberUtils.floatToIntColor(this.vertices[2]);
        Color color = this.color;
        color.r = (float)(intBits & 0xFF) / 255.0f;
        color.g = (float)(intBits >>> 8 & 0xFF) / 255.0f;
        color.b = (float)(intBits >>> 16 & 0xFF) / 255.0f;
        color.a = (float)(intBits >>> 24 & 0xFF) / 255.0f;
        return color;
    }

    public void setRegion(PolygonRegion region) {
        this.region = region;
        float[] regionVertices = region.vertices;
        float[] textureCoords = region.textureCoords;
        if (this.vertices == null || regionVertices.length != this.vertices.length) {
            this.vertices = new float[regionVertices.length / 2 * 5];
        }
        float[] vertices = this.vertices;
        int i = 0;
        int v = 2;
        int n = regionVertices.length;
        while (i < n) {
            vertices[v] = this.color.toFloatBits();
            vertices[v + 1] = textureCoords[i];
            vertices[v + 2] = textureCoords[i + 1];
            i += 2;
            v += 5;
        }
        this.dirty = true;
    }

    public PolygonRegion getRegion() {
        return this.region;
    }
}

