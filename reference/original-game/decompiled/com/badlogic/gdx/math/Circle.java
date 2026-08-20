/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Shape2D;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.NumberUtils;
import java.io.Serializable;

public class Circle
implements Serializable,
Shape2D {
    public float x;
    public float y;
    public float radius;

    public Circle() {
    }

    public Circle(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public Circle(Vector2 position, float radius) {
        this.x = position.x;
        this.y = position.y;
        this.radius = radius;
    }

    public Circle(Circle circle) {
        this.x = circle.x;
        this.y = circle.y;
        this.radius = circle.radius;
    }

    public Circle(Vector2 center, Vector2 edge) {
        this.x = center.x;
        this.y = center.y;
        this.radius = Vector2.len(center.x - edge.x, center.y - edge.y);
    }

    public void set(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public void set(Vector2 position, float radius) {
        this.x = position.x;
        this.y = position.y;
        this.radius = radius;
    }

    public void set(Circle circle) {
        this.x = circle.x;
        this.y = circle.y;
        this.radius = circle.radius;
    }

    public void set(Vector2 center, Vector2 edge) {
        this.x = center.x;
        this.y = center.y;
        this.radius = Vector2.len(center.x - edge.x, center.y - edge.y);
    }

    public void setPosition(Vector2 position) {
        this.x = position.x;
        this.y = position.y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public boolean contains(float x, float y) {
        return (x = this.x - x) * x + (y = this.y - y) * y <= this.radius * this.radius;
    }

    @Override
    public boolean contains(Vector2 point) {
        float dx = this.x - point.x;
        float dy = this.y - point.y;
        return dx * dx + dy * dy <= this.radius * this.radius;
    }

    public boolean contains(Circle c) {
        float radiusDiff = this.radius - c.radius;
        if (radiusDiff < 0.0f) {
            return false;
        }
        float dx = this.x - c.x;
        float dy = this.y - c.y;
        float dst = dx * dx + dy * dy;
        float radiusSum = this.radius + c.radius;
        return !(radiusDiff * radiusDiff < dst) && dst < radiusSum * radiusSum;
    }

    public boolean overlaps(Circle c) {
        float dx = this.x - c.x;
        float dy = this.y - c.y;
        float distance = dx * dx + dy * dy;
        float radiusSum = this.radius + c.radius;
        return distance < radiusSum * radiusSum;
    }

    public String toString() {
        return this.x + "," + this.y + "," + this.radius;
    }

    public float circumference() {
        return this.radius * ((float)Math.PI * 2);
    }

    public float area() {
        return this.radius * this.radius * (float)Math.PI;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Circle c = (Circle)o;
        return this.x == c.x && this.y == c.y && this.radius == c.radius;
    }

    public int hashCode() {
        int prime = 41;
        int result = 1;
        result = 41 * result + NumberUtils.floatToRawIntBits(this.radius);
        result = 41 * result + NumberUtils.floatToRawIntBits(this.x);
        result = 41 * result + NumberUtils.floatToRawIntBits(this.y);
        return result;
    }
}

