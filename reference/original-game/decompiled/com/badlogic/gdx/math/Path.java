/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

public interface Path<T> {
    public T derivativeAt(T var1, float var2);

    public T valueAt(T var1, float var2);

    public float approximate(T var1);

    public float locate(T var1);

    public float approxLength(int var1);
}

