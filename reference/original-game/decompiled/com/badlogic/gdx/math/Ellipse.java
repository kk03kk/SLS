/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Shape2D;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.NumberUtils;
import java.io.Serializable;

public class Ellipse
implements Serializable,
Shape2D {
    public float x;
    public float y;
    public float width;
    public float height;
    private static final long serialVersionUID = 7381533206532032099L;

    public Ellipse() {
    }

    public Ellipse(Ellipse ellipse) {
        this.x = ellipse.x;
        this.y = ellipse.y;
        this.width = ellipse.width;
        this.height = ellipse.height;
    }

    public Ellipse(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Ellipse(Vector2 position, float width, float height) {
        this.x = position.x;
        this.y = position.y;
        this.width = width;
        this.height = height;
    }

    public Ellipse(Vector2 position, Vector2 size) {
        this.x = position.x;
        this.y = position.y;
        this.width = size.x;
        this.height = size.y;
    }

    public Ellipse(Circle circle) {
        this.x = circle.x;
        this.y = circle.y;
        this.width = circle.radius;
        this.height = circle.radius;
    }

    @Override
    public boolean contains(float x, float y) {
        return (x -= this.x) * x / (this.width * 0.5f * this.width * 0.5f) + (y -= this.y) * y / (this.height * 0.5f * this.height * 0.5f) <= 1.0f;
    }

    @Override
    public boolean contains(Vector2 point) {
        return this.contains(point.x, point.y);
    }

    public void set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void set(Ellipse ellipse) {
        this.x = ellipse.x;
        this.y = ellipse.y;
        this.width = ellipse.width;
        this.height = ellipse.height;
    }

    public void set(Circle circle) {
        this.x = circle.x;
        this.y = circle.y;
        this.width = circle.radius;
        this.height = circle.radius;
    }

    public void set(Vector2 position, Vector2 size) {
        this.x = position.x;
        this.y = position.y;
        this.width = size.x;
        this.height = size.y;
    }

    public Ellipse setPosition(Vector2 position) {
        this.x = position.x;
        this.y = position.y;
        return this;
    }

    public Ellipse setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Ellipse setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public float area() {
        return (float)Math.PI * (this.width * this.height) / 4.0f;
    }

    public float circumference() {
        float a = this.width / 2.0f;
        float b = this.height / 2.0f;
        if (a * 3.0f > b || b * 3.0f > a) {
            return (float)(3.1415927410125732 * ((double)(3.0f * (a + b)) - Math.sqrt((3.0f * a + b) * (a + 3.0f * b))));
        }
        return (float)(6.2831854820251465 * Math.sqrt((a * a + b * b) / 2.0f));
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Ellipse e = (Ellipse)o;
        return this.x == e.x && this.y == e.y && this.width == e.width && this.height == e.height;
    }

    public int hashCode() {
        int prime = 53;
        int result = 1;
        result = 53 * result + NumberUtils.floatToRawIntBits(this.height);
        result = 53 * result + NumberUtils.floatToRawIntBits(this.width);
        result = 53 * result + NumberUtils.floatToRawIntBits(this.x);
        result = 53 * result + NumberUtils.floatToRawIntBits(this.y);
        return result;
    }
}

