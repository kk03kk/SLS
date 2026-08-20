/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

public interface CubemapData {
    public boolean isPrepared();

    public void prepare();

    public void consumeCubemapData();

    public int getWidth();

    public int getHeight();

    public boolean isManaged();
}

