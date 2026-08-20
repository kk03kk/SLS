/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;

public interface MapRenderer {
    public void setView(OrthographicCamera var1);

    public void setView(Matrix4 var1, float var2, float var3, float var4, float var5);

    public void render();

    public void render(int[] var1);
}

