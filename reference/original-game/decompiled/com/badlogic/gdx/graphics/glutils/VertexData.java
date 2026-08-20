/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import java.nio.FloatBuffer;

public interface VertexData
extends Disposable {
    public int getNumVertices();

    public int getNumMaxVertices();

    public VertexAttributes getAttributes();

    public void setVertices(float[] var1, int var2, int var3);

    public void updateVertices(int var1, float[] var2, int var3, int var4);

    public FloatBuffer getBuffer();

    public void bind(ShaderProgram var1);

    public void bind(ShaderProgram var1, int[] var2);

    public void unbind(ShaderProgram var1);

    public void unbind(ShaderProgram var1, int[] var2);

    public void invalidate();

    @Override
    public void dispose();
}

