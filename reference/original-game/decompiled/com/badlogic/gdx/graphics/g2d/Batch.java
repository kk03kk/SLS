/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

public interface Batch
extends Disposable {
    public static final int X1 = 0;
    public static final int Y1 = 1;
    public static final int C1 = 2;
    public static final int U1 = 3;
    public static final int V1 = 4;
    public static final int X2 = 5;
    public static final int Y2 = 6;
    public static final int C2 = 7;
    public static final int U2 = 8;
    public static final int V2 = 9;
    public static final int X3 = 10;
    public static final int Y3 = 11;
    public static final int C3 = 12;
    public static final int U3 = 13;
    public static final int V3 = 14;
    public static final int X4 = 15;
    public static final int Y4 = 16;
    public static final int C4 = 17;
    public static final int U4 = 18;
    public static final int V4 = 19;

    public void begin();

    public void end();

    public void setColor(Color var1);

    public void setColor(float var1, float var2, float var3, float var4);

    public void setColor(float var1);

    public Color getColor();

    public float getPackedColor();

    public void draw(Texture var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, int var11, int var12, int var13, int var14, boolean var15, boolean var16);

    public void draw(Texture var1, float var2, float var3, float var4, float var5, int var6, int var7, int var8, int var9, boolean var10, boolean var11);

    public void draw(Texture var1, float var2, float var3, int var4, int var5, int var6, int var7);

    public void draw(Texture var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9);

    public void draw(Texture var1, float var2, float var3);

    public void draw(Texture var1, float var2, float var3, float var4, float var5);

    public void draw(Texture var1, float[] var2, int var3, int var4);

    public void draw(TextureRegion var1, float var2, float var3);

    public void draw(TextureRegion var1, float var2, float var3, float var4, float var5);

    public void draw(TextureRegion var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10);

    public void draw(TextureRegion var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, boolean var11);

    public void draw(TextureRegion var1, float var2, float var3, Affine2 var4);

    public void flush();

    public void disableBlending();

    public void enableBlending();

    public void setBlendFunction(int var1, int var2);

    public int getBlendSrcFunc();

    public int getBlendDstFunc();

    public Matrix4 getProjectionMatrix();

    public Matrix4 getTransformMatrix();

    public void setProjectionMatrix(Matrix4 var1);

    public void setTransformMatrix(Matrix4 var1);

    public void setShader(ShaderProgram var1);

    public ShaderProgram getShader();

    public boolean isBlendingEnabled();

    public boolean isDrawing();
}

