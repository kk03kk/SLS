/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class AmbientCubemap {
    public final float[] data;

    private static final float clamp(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public AmbientCubemap() {
        this.data = new float[18];
    }

    public AmbientCubemap(float[] copyFrom) {
        if (copyFrom.length != 18) {
            throw new GdxRuntimeException("Incorrect array size");
        }
        this.data = new float[copyFrom.length];
        System.arraycopy(copyFrom, 0, this.data, 0, this.data.length);
    }

    public AmbientCubemap(AmbientCubemap copyFrom) {
        this(copyFrom.data);
    }

    public AmbientCubemap set(float[] values) {
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] = values[i];
        }
        return this;
    }

    public AmbientCubemap set(AmbientCubemap other) {
        return this.set(other.data);
    }

    public AmbientCubemap set(Color color) {
        return this.set(color.r, color.g, color.b);
    }

    public AmbientCubemap set(float r, float g, float b) {
        int idx = 0;
        while (idx < this.data.length) {
            this.data[idx++] = r;
            this.data[idx++] = g;
            this.data[idx++] = b;
        }
        return this;
    }

    public Color getColor(Color out, int side) {
        return out.set(this.data[side *= 3], this.data[side + 1], this.data[side + 2], 1.0f);
    }

    public AmbientCubemap clear() {
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] = 0.0f;
        }
        return this;
    }

    public AmbientCubemap clamp() {
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] = AmbientCubemap.clamp(this.data[i]);
        }
        return this;
    }

    public AmbientCubemap add(float r, float g, float b) {
        int idx = 0;
        while (idx < this.data.length) {
            int n = idx++;
            this.data[n] = this.data[n] + r;
            int n2 = idx++;
            this.data[n2] = this.data[n2] + g;
            int n3 = idx++;
            this.data[n3] = this.data[n3] + b;
        }
        return this;
    }

    public AmbientCubemap add(Color color) {
        return this.add(color.r, color.g, color.b);
    }

    public AmbientCubemap add(float r, float g, float b, float x, float y, float z) {
        int idx;
        float x2 = x * x;
        float y2 = y * y;
        float z2 = z * z;
        float d = x2 + y2 + z2;
        if (d == 0.0f) {
            return this;
        }
        d = 1.0f / d * (d + 1.0f);
        float rd = r * d;
        float gd = g * d;
        float bd = b * d;
        int n = idx = x > 0.0f ? 0 : 3;
        this.data[n] = this.data[n] + x2 * rd;
        int n2 = idx + 1;
        this.data[n2] = this.data[n2] + x2 * gd;
        int n3 = idx + 2;
        this.data[n3] = this.data[n3] + x2 * bd;
        int n4 = idx = y > 0.0f ? 6 : 9;
        this.data[n4] = this.data[n4] + y2 * rd;
        int n5 = idx + 1;
        this.data[n5] = this.data[n5] + y2 * gd;
        int n6 = idx + 2;
        this.data[n6] = this.data[n6] + y2 * bd;
        int n7 = idx = z > 0.0f ? 12 : 15;
        this.data[n7] = this.data[n7] + z2 * rd;
        int n8 = idx + 1;
        this.data[n8] = this.data[n8] + z2 * gd;
        int n9 = idx + 2;
        this.data[n9] = this.data[n9] + z2 * bd;
        return this;
    }

    public AmbientCubemap add(Color color, Vector3 direction) {
        return this.add(color.r, color.g, color.b, direction.x, direction.y, direction.z);
    }

    public AmbientCubemap add(float r, float g, float b, Vector3 direction) {
        return this.add(r, g, b, direction.x, direction.y, direction.z);
    }

    public AmbientCubemap add(Color color, float x, float y, float z) {
        return this.add(color.r, color.g, color.b, x, y, z);
    }

    public AmbientCubemap add(Color color, Vector3 point, Vector3 target) {
        return this.add(color.r, color.g, color.b, target.x - point.x, target.y - point.y, target.z - point.z);
    }

    public AmbientCubemap add(Color color, Vector3 point, Vector3 target, float intensity) {
        float t = intensity / (1.0f + target.dst(point));
        return this.add(color.r * t, color.g * t, color.b * t, target.x - point.x, target.y - point.y, target.z - point.z);
    }

    public String toString() {
        String result = "";
        for (int i = 0; i < this.data.length; i += 3) {
            result = result + Float.toString(this.data[i]) + ", " + Float.toString(this.data[i + 1]) + ", " + Float.toString(this.data[i + 2]) + "\n";
        }
        return result;
    }
}

