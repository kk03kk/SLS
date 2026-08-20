/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

public interface Layout {
    public void layout();

    public void invalidate();

    public void invalidateHierarchy();

    public void validate();

    public void pack();

    public void setFillParent(boolean var1);

    public void setLayoutEnabled(boolean var1);

    public float getMinWidth();

    public float getMinHeight();

    public float getPrefWidth();

    public float getPrefHeight();

    public float getMaxWidth();

    public float getMaxHeight();
}

