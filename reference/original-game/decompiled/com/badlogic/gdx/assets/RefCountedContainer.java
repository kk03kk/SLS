/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets;

public class RefCountedContainer {
    Object object;
    int refCount = 1;

    public RefCountedContainer(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object must not be null");
        }
        this.object = object;
    }

    public void incRefCount() {
        ++this.refCount;
    }

    public void decRefCount() {
        --this.refCount;
    }

    public int getRefCount() {
        return this.refCount;
    }

    public void setRefCount(int refCount) {
        this.refCount = refCount;
    }

    public <T> T getObject(Class<T> type) {
        return (T)this.object;
    }

    public void setObject(Object asset) {
        this.object = asset;
    }
}

