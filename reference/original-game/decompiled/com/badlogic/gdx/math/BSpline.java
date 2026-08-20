/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Path;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.utils.Array;

public class BSpline<T extends Vector<T>>
implements Path<T> {
    private static final float d6 = 0.16666667f;
    public T[] controlPoints;
    public Array<T> knots;
    public int degree;
    public boolean continuous;
    public int spanCount;
    private T tmp;
    private T tmp2;
    private T tmp3;

    public static <T extends Vector<T>> T cubic(T out, float t, T[] points, boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - 3;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return (T)BSpline.cubic(out, (int)i, (float)(u -= (float)i), points, (boolean)continuous, tmp);
    }

    public static <T extends Vector<T>> T cubic_derivative(T out, float t, T[] points, boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - 3;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return (T)BSpline.cubic(out, (int)i, (float)(u -= (float)i), points, (boolean)continuous, tmp);
    }

    public static <T extends Vector<T>> T cubic(T out, int i, float u, T[] points, boolean continuous, T tmp) {
        int n = points.length;
        float dt = 1.0f - u;
        float t2 = u * u;
        float t3 = t2 * u;
        out.set(points[i]).scl((float)((3.0f * t3 - 6.0f * t2 + 4.0f) * 0.16666667f));
        if (continuous || i > 0) {
            out.add(tmp.set(points[(n + i - 1) % n]).scl((float)(dt * dt * dt * 0.16666667f)));
        }
        if (continuous || i < n - 1) {
            out.add(tmp.set(points[(i + 1) % n]).scl((float)((-3.0f * t3 + 3.0f * t2 + 3.0f * u + 1.0f) * 0.16666667f)));
        }
        if (continuous || i < n - 2) {
            out.add(tmp.set(points[(i + 2) % n]).scl((float)(t3 * 0.16666667f)));
        }
        return (T)out;
    }

    public static <T extends Vector<T>> T cubic_derivative(T out, int i, float u, T[] points, boolean continuous, T tmp) {
        int n = points.length;
        float dt = 1.0f - u;
        float t2 = u * u;
        float t3 = t2 * u;
        out.set(points[i]).scl((float)(1.5f * t2 - 2.0f * u));
        if (continuous || i > 0) {
            out.add(tmp.set(points[(n + i - 1) % n]).scl((float)(-0.5f * dt * dt)));
        }
        if (continuous || i < n - 1) {
            out.add(tmp.set(points[(i + 1) % n]).scl((float)(-1.5f * t2 + u + 0.5f)));
        }
        if (continuous || i < n - 2) {
            out.add(tmp.set(points[(i + 2) % n]).scl((float)(0.5f * t2)));
        }
        return (T)out;
    }

    public static <T extends Vector<T>> T calculate(T out, float t, T[] points, int degree, boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - degree;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return (T)BSpline.calculate(out, (int)i, (float)(u -= (float)i), points, (int)degree, (boolean)continuous, tmp);
    }

    public static <T extends Vector<T>> T derivative(T out, float t, T[] points, int degree, boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - degree;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return (T)BSpline.derivative(out, (int)i, (float)(u -= (float)i), points, (int)degree, (boolean)continuous, tmp);
    }

    public static <T extends Vector<T>> T calculate(T out, int i, float u, T[] points, int degree, boolean continuous, T tmp) {
        switch (degree) {
            case 3: {
                return (T)BSpline.cubic(out, (int)i, (float)u, points, (boolean)continuous, tmp);
            }
        }
        return out;
    }

    public static <T extends Vector<T>> T derivative(T out, int i, float u, T[] points, int degree, boolean continuous, T tmp) {
        switch (degree) {
            case 3: {
                return (T)BSpline.cubic_derivative(out, (int)i, (float)u, points, (boolean)continuous, tmp);
            }
        }
        return out;
    }

    public BSpline() {
    }

    public BSpline(T[] controlPoints, int degree, boolean continuous) {
        this.set((Vector[])controlPoints, degree, continuous);
    }

    public BSpline set(T[] controlPoints, int degree, boolean continuous) {
        if (this.tmp == null) {
            this.tmp = controlPoints[0].cpy();
        }
        if (this.tmp2 == null) {
            this.tmp2 = controlPoints[0].cpy();
        }
        if (this.tmp3 == null) {
            this.tmp3 = controlPoints[0].cpy();
        }
        this.controlPoints = controlPoints;
        this.degree = degree;
        this.continuous = continuous;
        int n = this.spanCount = continuous ? controlPoints.length : controlPoints.length - degree;
        if (this.knots == null) {
            this.knots = new Array(this.spanCount);
        } else {
            this.knots.clear();
            this.knots.ensureCapacity(this.spanCount);
        }
        for (int i = 0; i < this.spanCount; ++i) {
            this.knots.add(BSpline.calculate(controlPoints[0].cpy(), (int)(continuous ? i : (int)((float)i + 0.5f * (float)degree)), (float)0.0f, controlPoints, (int)degree, (boolean)continuous, this.tmp));
        }
        return this;
    }

    @Override
    public T valueAt(T out, float t) {
        int n = this.spanCount;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return this.valueAt(out, i, u -= (float)i);
    }

    public T valueAt(T out, int span, float u) {
        return (T)BSpline.calculate(out, (int)(this.continuous ? span : span + (int)((float)this.degree * 0.5f)), (float)u, this.controlPoints, (int)this.degree, (boolean)this.continuous, this.tmp);
    }

    @Override
    public T derivativeAt(T out, float t) {
        int n = this.spanCount;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return this.derivativeAt(out, i, u -= (float)i);
    }

    public T derivativeAt(T out, int span, float u) {
        return (T)BSpline.derivative(out, (int)(this.continuous ? span : span + (int)((float)this.degree * 0.5f)), (float)u, this.controlPoints, (int)this.degree, (boolean)this.continuous, this.tmp);
    }

    public int nearest(T in) {
        return this.nearest(in, 0, this.spanCount);
    }

    public int nearest(T in, int start, int count) {
        while (start < 0) {
            start += this.spanCount;
        }
        int result = start % this.spanCount;
        float dst = in.dst2((Vector)((Vector)this.knots.get(result)));
        for (int i = 1; i < count; ++i) {
            int idx = (start + i) % this.spanCount;
            float d = in.dst2((Vector)((Vector)this.knots.get(idx)));
            if (!(d < dst)) continue;
            dst = d;
            result = idx;
        }
        return result;
    }

    @Override
    public float approximate(T v) {
        return this.approximate(v, this.nearest(v));
    }

    public float approximate(T in, int start, int count) {
        return this.approximate(in, this.nearest(in, start, count));
    }

    public float approximate(T in, int near) {
        Vector P3;
        Vector P2;
        Vector P1;
        int n = near;
        Vector nearest = (Vector)this.knots.get(n);
        Vector previous = (Vector)this.knots.get(n > 0 ? n - 1 : this.spanCount - 1);
        Vector next = (Vector)this.knots.get((n + 1) % this.spanCount);
        float dstPrev2 = in.dst2((Vector)previous);
        float dstNext2 = in.dst2((Vector)next);
        if (dstNext2 < dstPrev2) {
            P1 = nearest;
            P2 = next;
            P3 = in;
        } else {
            P1 = previous;
            P2 = nearest;
            P3 = in;
            n = n > 0 ? n - 1 : this.spanCount - 1;
        }
        float L1Sqr = P1.dst2(P2);
        float L2Sqr = P3.dst2((Vector)P2);
        float L3Sqr = P3.dst2((Vector)P1);
        float L1 = (float)Math.sqrt(L1Sqr);
        float s = (L2Sqr + L1Sqr - L3Sqr) / (2.0f * L1);
        float u = MathUtils.clamp((L1 - s) / L1, 0.0f, 1.0f);
        return ((float)n + u) / (float)this.spanCount;
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

