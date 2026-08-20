/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.utils.Disposable;
import java.nio.ShortBuffer;

public interface IndexData
extends Disposable {
    public int getNumIndices();

    public int getNumMaxIndices();

    public void setIndices(short[] var1, int var2, int var3);

    public void setIndices(ShortBuffer var1);

    public void updateIndices(int var1, short[] var2, int var3, int var4);

    public ShortBuffer getBuffer();

    public void bind();

    public void unbind();

    public void invalidate();

    @Override
    public void dispose();
}

