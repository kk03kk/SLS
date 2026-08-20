/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;

public interface ImmediateModeRenderer {
    public void begin(Matrix4 var1, int var2);

    public void flush();

    public void color(Color var1);

    public void color(float var1, float var2, float var3, float var4);

    public void color(float var1);

    public void texCoord(float var1, float var2);

    public void normal(float var1, float var2, float var3);

    public void vertex(float var1, float var2, float var3);

    public void end();

    public int getNumVertices();

    public int getMaxVertices();

    public void dispose();
}

