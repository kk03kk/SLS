/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Path;
import com.badlogic.gdx.math.Vector;

public class CatmullRomSpline<T extends Vector<T>>
implements Path<T> {
    public T[] controlPoints;
    public boolean continuous;
    public int spanCount;
    private T tmp;
    private T tmp2;
    private T tmp3;

    public static <T extends Vector<T>> T calculate(T out, float t, T[] points, boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - 3;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return (T)CatmullRomSpline.calculate(out, (int)i, (float)(u -= (float)i), points, (boolean)continuous, tmp);
    }

    public static <T extends Vector<T>> T calculate(T out, int i, float u, T[] points, boolean continuous, T tmp) {
        int n = points.length;
        float u2 = u * u;
        float u3 = u2 * u;
        out.set(points[i]).scl((float)(1.5f * u3 - 2.5f * u2 + 1.0f));
        if (continuous || i > 0) {
            out.add(tmp.set(points[(n + i - 1) % n]).scl((float)(-0.5f * u3 + u2 - 0.5f * u)));
        }
        if (continuous || i < n - 1) {
            out.add(tmp.set(points[(i + 1) % n]).scl((float)(-1.5f * u3 + 2.0f * u2 + 0.5f * u)));
        }
        if (continuous || i < n - 2) {
            out.add(tmp.set(points[(i + 2) % n]).scl((float)(0.5f * u3 - 0.5f * u2)));
        }
        return (T)out;
    }

    public static <T extends Vector<T>> T derivative(T out, float t, T[] points, boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - 3;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return (T)CatmullRomSpline.derivative(out, (int)i, (float)(u -= (float)i), points, (boolean)continuous, tmp);
    }

    public static <T extends Vector<T>> T derivative(T out, int i, float u, T[] points, boolean continuous, T tmp) {
        int n = points.length;
        float u2 = u * u;
        out.set(points[i]).scl((float)(-u * 5.0f + u2 * 4.5f));
        if (continuous || i > 0) {
            out.add(tmp.set(points[(n + i - 1) % n]).scl((float)(-0.5f + u * 2.0f - u2 * 1.5f)));
        }
        if (continuous || i < n - 1) {
            out.add(tmp.set(points[(i + 1) % n]).scl((float)(0.5f + u * 4.0f - u2 * 4.5f)));
        }
        if (continuous || i < n - 2) {
            out.add(tmp.set(points[(i + 2) % n]).scl((float)(-u + u2 * 1.5f)));
        }
        return (T)out;
    }

    public CatmullRomSpline() {
    }

    public CatmullRomSpline(T[] controlPoints, boolean continuous) {
        this.set((Vector[])controlPoints, continuous);
    }

    public CatmullRomSpline set(T[] controlPoints, boolean continuous) {
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
        this.continuous = continuous;
        this.spanCount = continuous ? controlPoints.length : controlPoints.length - 3;
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
        return (T)CatmullRomSpline.calculate(out, (int)(this.continuous ? span : span + 1), (float)u, this.controlPoints, (boolean)this.continuous, this.tmp);
    }

    @Override
    public T derivativeAt(T out, float t) {
        int n = this.spanCount;
        float u = t * (float)n;
        int i = t >= 1.0f ? n - 1 : (int)u;
        return this.derivativeAt(out, i, u -= (float)i);
    }

    public T derivativeAt(T out, int span, float u) {
        return (T)CatmullRomSpline.derivative(out, (int)(this.continuous ? span : span + 1), (float)u, this.controlPoints, (boolean)this.continuous, this.tmp);
    }

    public int nearest(T in) {
        return this.nearest(in, 0, this.spanCount);
    }

    public int nearest(T in, int start, int count) {
        while (start < 0) {
            start += this.spanCount;
        }
        int result = start % this.spanCount;
        float dst = in.dst2(this.controlPoints[result]);
        for (int i = 1; i < count; ++i) {
            int idx = (start + i) % this.spanCount;
            float d = in.dst2(this.controlPoints[idx]);
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
        T P3;
        T P2;
        T P1;
        int n = near;
        T nearest = this.controlPoints[n];
        T previous = this.controlPoints[n > 0 ? n - 1 : this.spanCount - 1];
        T next = this.controlPoints[(n + 1) % this.spanCount];
        float dstPrev2 = in.dst2(previous);
        float dstNext2 = in.dst2(next);
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
        float L2Sqr = P3.dst2(P2);
        float L3Sqr = P3.dst2(P1);
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

