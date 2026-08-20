/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.reflect.ClassReflection;

public class BaseDrawable
implements Drawable {
    private String name;
    private float leftWidth;
    private float rightWidth;
    private float topHeight;
    private float bottomHeight;
    private float minWidth;
    private float minHeight;

    public BaseDrawable() {
    }

    public BaseDrawable(Drawable drawable) {
        if (drawable instanceof BaseDrawable) {
            this.name = ((BaseDrawable)drawable).getName();
        }
        this.leftWidth = drawable.getLeftWidth();
        this.rightWidth = drawable.getRightWidth();
        this.topHeight = drawable.getTopHeight();
        this.bottomHeight = drawable.getBottomHeight();
        this.minWidth = drawable.getMinWidth();
        this.minHeight = drawable.getMinHeight();
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
    }

    @Override
    public float getLeftWidth() {
        return this.leftWidth;
    }

    @Override
    public void setLeftWidth(float leftWidth) {
        this.leftWidth = leftWidth;
    }

    @Override
    public float getRightWidth() {
        return this.rightWidth;
    }

    @Override
    public void setRightWidth(float rightWidth) {
        this.rightWidth = rightWidth;
    }

    @Override
    public float getTopHeight() {
        return this.topHeight;
    }

    @Override
    public void setTopHeight(float topHeight) {
        this.topHeight = topHeight;
    }

    @Override
    public float getBottomHeight() {
        return this.bottomHeight;
    }

    @Override
    public void setBottomHeight(float bottomHeight) {
        this.bottomHeight = bottomHeight;
    }

    @Override
    public float getMinWidth() {
        return this.minWidth;
    }

    @Override
    public void setMinWidth(float minWidth) {
        this.minWidth = minWidth;
    }

    @Override
    public float getMinHeight() {
        return this.minHeight;
    }

    @Override
    public void setMinHeight(float minHeight) {
        this.minHeight = minHeight;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        if (this.name == null) {
            return ClassReflection.getSimpleName(this.getClass());
        }
        return this.name;
    }
}

