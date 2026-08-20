/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Interpolation;

public interface Vector<T extends Vector<T>> {
    public T cpy();

    public float len();

    public float len2();

    public T limit(float var1);

    public T limit2(float var1);

    public T setLength(float var1);

    public T setLength2(float var1);

    public T clamp(float var1, float var2);

    public T set(T var1);

    public T sub(T var1);

    public T nor();

    public T add(T var1);

    public float dot(T var1);

    public T scl(float var1);

    public T scl(T var1);

    public float dst(T var1);

    public float dst2(T var1);

    public T lerp(T var1, float var2);

    public T interpolate(T var1, float var2, Interpolation var3);

    public T setToRandomDirection();

    public boolean isUnit();

    public boolean isUnit(float var1);

    public boolean isZero();

    public boolean isZero(float var1);

    public boolean isOnLine(T var1, float var2);

    public boolean isOnLine(T var1);

    public boolean isCollinear(T var1, float var2);

    public boolean isCollinear(T var1);

    public boolean isCollinearOpposite(T var1, float var2);

    public boolean isCollinearOpposite(T var1);

    public boolean isPerpendicular(T var1);

    public boolean isPerpendicular(T var1, float var2);

    public boolean hasSameDirection(T var1);

    public boolean hasOppositeDirection(T var1);

    public boolean epsilonEquals(T var1, float var2);

    public T mulAdd(T var1, float var2);

    public T mulAdd(T var1, T var2);

    public T setZero();
}

