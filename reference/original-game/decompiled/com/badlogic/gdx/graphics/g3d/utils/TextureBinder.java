/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;

public interface TextureBinder {
    public void begin();

    public void end();

    public int bind(TextureDescriptor var1);

    public int bind(GLTexture var1);

    public int getBindCount();

    public int getReuseCount();

    public void resetCounts();
}

