/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public abstract class FlushablePool<T>
extends Pool<T> {
    protected Array<T> obtained = new Array();

    public FlushablePool() {
    }

    public FlushablePool(int initialCapacity) {
        super(initialCapacity);
    }

    public FlushablePool(int initialCapacity, int max) {
        super(initialCapacity, max);
    }

    @Override
    public T obtain() {
        Object result = super.obtain();
        this.obtained.add(result);
        return result;
    }

    public void flush() {
        super.freeAll(this.obtained);
        this.obtained.clear();
    }

    @Override
    public void free(T object) {
        this.obtained.removeValue(object, true);
        super.free(object);
    }

    @Override
    public void freeAll(Array<T> objects) {
        this.obtained.removeAll(objects, true);
        super.freeAll(objects);
    }
}

