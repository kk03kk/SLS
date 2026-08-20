/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.graphics.g2d.Batch;

public interface Drawable {
    public void draw(Batch var1, float var2, float var3, float var4, float var5);

    public float getLeftWidth();

    public void setLeftWidth(float var1);

    public float getRightWidth();

    public void setRightWidth(float var1);

    public float getTopHeight();

    public void setTopHeight(float var1);

    public float getBottomHeight();

    public void setBottomHeight(float var1);

    public float getMinWidth();

    public void setMinWidth(float var1);

    public float getMinHeight();

    public void setMinHeight(float var1);
}

