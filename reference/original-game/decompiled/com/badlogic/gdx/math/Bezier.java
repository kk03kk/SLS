/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Path;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Bezier<T extends Vector<T>>
implements Path<T> {
    public Array<T> points = new Array();
    private T tmp;
    private T tmp2;
    private T tmp3;

    public static <T extends Vector<T>> T linear(T out, float t, T p0, T p1, T tmp) {
        return out.set(p0).scl((float)(1.0f - t)).add(tmp.set(p1).scl((float)t));
    }

    public static <T extends Vector<T>> T linear_derivative(T out, float t, T p0, T p1, T tmp) {
        return out.set(p1).sub(p0);
    }

    public static <T extends Vector<T>> T quadratic(T out, float t, T p0, T p1, T p2, T tmp) {
        float dt = 1.0f - t;
        return out.set(p0).scl((float)(dt * dt)).add(tmp.set(p1).scl((float)(2.0f * dt * t))).add(tmp.set(p2).scl((float)(t * t)));
    }

    public static <T extends Vector<T>> T quadratic_derivative(T out, float t, T p0, T p1, T p2, T tmp) {
        float dt = 1.0f - t;
        return out.set(p1).sub(p0).scl((float)2.0f).scl(1.0f - t).add(tmp.set(p2).sub(p1).scl((float)t).scl(2.0f));
    }

    public static <T extends Vector<T>> T cubic(T out, float t, T p0, T p1, T p2, T p3, T tmp) {
        float dt = 1.0f - t;
        float dt2 = dt * dt;
        float t2 = t * t;
        return out.set(p0).scl((float)(dt2 * dt)).add(tmp.set(p1).scl((float)(3.0f * dt2 * t))).add(tmp.set(p2).scl((float)(3.0f * dt * t2))).add(tmp.set(p3).scl((float)(t2 * t)));
    }

    public static <T extends Vector<T>> T cubic_derivative(T out, float t, T p0, T p1, T p2, T p3, T tmp) {
        float dt = 1.0f - t;
        float dt2 = dt * dt;
        float t2 = t * t;
        return out.set(p1).sub(p0).scl((float)(dt2 * 3.0f)).add(tmp.set(p2).sub(p1).scl((float)(dt * t * 6.0f))).add(tmp.set(p3).sub(p2).scl((float)(t2 * 3.0f)));
    }

    public Bezier() {
    }

    public Bezier(T ... points) {
        this.set((Vector[])points);
    }

    public Bezier(T[] points, int offset, int length) {
        this.set((Vector[])points, offset, length);
    }

    public Bezier(Array<T> points, int offset, int length) {
        this.set(points, offset, length);
    }

    public Bezier set(T ... points) {
        return this.set((Vector[])points, 0, points.length);
    }

    public Bezier set(T[] points, int offset, int length) {
        if (length < 2 || length > 4) {
            throw new GdxRuntimeException("Only first, second and third degree Bezier curves are supported.");
        }
        if (this.tmp == null) {
            this.tmp = points[0].cpy();
        }
        if (this.tmp2 == null) {
            this.tmp2 = points[0].cpy();
        }
        if (this.tmp3 == null) {
            this.tmp3 = points[0].cpy();
        }
        this.points.clear();
        this.points.addAll(points, offset, length);
        return this;
    }

    public Bezier set(Array<T> points, int offset, int length) {
        if (length < 2 || length > 4) {
            throw new GdxRuntimeException("Only first, second and third degree Bezier curves are supported.");
        }
        if (this.tmp == null) {
            this.tmp = ((Vector)points.get(0)).cpy();
        }
        this.points.clear();
        this.points.addAll(points, offset, length);
        return this;
    }

    @Override
    public T valueAt(T out, float t) {
        int n = this.points.size;
        if (n == 2) {
            Bezier.linear(out, t, (Vector)this.points.get(0), (Vector)this.points.get(1), this.tmp);
        } else if (n == 3) {
            Bezier.quadratic(out, t, (Vector)this.points.get(0), (Vector)this.points.get(1), (Vector)this.points.get(2), this.tmp);
        } else if (n == 4) {
            Bezier.cubic(out, t, (Vector)this.points.get(0), (Vector)this.points.get(1), (Vector)this.points.get(2), (Vector)this.points.get(3), this.tmp);
        }
        return out;
    }

    @Override
    public T derivativeAt(T out, float t) {
        int n = this.points.size;
        if (n == 2) {
            Bezier.linear_derivative(out, t, (Vector)this.points.get(0), (Vector)this.points.get(1), this.tmp);
        } else if (n == 3) {
            Bezier.quadratic_derivative(out, t, (Vector)this.points.get(0), (Vector)this.points.get(1), (Vector)this.points.get(2), this.tmp);
        } else if (n == 4) {
            Bezier.cubic_derivative(out, t, (Vector)this.points.get(0), (Vector)this.points.get(1), (Vector)this.points.get(2), (Vector)this.points.get(3), this.tmp);
        }
        return out;
    }

    @Override
    public float approximate(T v) {
        Vector p1 = (Vector)this.points.get(0);
        Vector p2 = (Vector)this.points.get(this.points.size - 1);
        Vector p3 = v;
        float l1Sqr = p1.dst2(p2);
        float l2Sqr = p3.dst2((Vector)p2);
        float l3Sqr = p3.dst2((Vector)p1);
        float l1 = (float)Math.sqrt(l1Sqr);
        float s = (l2Sqr + l1Sqr - l3Sqr) / (2.0f * l1);
        return MathUtils.clamp((l1 - s) / l1, 0.0f, 1.0f);
    }

    @Override
    public float locate(T v) {
        return this.approximate(v);
    }

    @Override
    public float approxLength(int samples) {
        float tempLength = 0.0f;
        for (int i = 0; i < samples; ++i) {
            this.tmp2.set(this.tmp3);
            this.valueAt(this.tmp3, (float)i / ((float)samples - 1.0f));
            if (i <= 0) continue;
            tempLength += this.tmp2.dst(this.tmp3);
        }
        return tempLength;
    }
}

