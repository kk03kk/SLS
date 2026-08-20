/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

public class MoveToAction
extends TemporalAction {
    private float startX;
    private float startY;
    private float endX;
    private float endY;
    private int alignment = 12;

    @Override
    protected void begin() {
        this.startX = this.target.getX(this.alignment);
        this.startY = this.target.getY(this.alignment);
    }

    @Override
    protected void update(float percent) {
        this.target.setPosition(this.startX + (this.endX - this.startX) * percent, this.startY + (this.endY - this.startY) * percent, this.alignment);
    }

    @Override
    public void reset() {
        super.reset();
        this.alignment = 12;
    }

    public void setPosition(float x, float y) {
        this.endX = x;
        this.endY = y;
    }

    public void setPosition(float x, float y, int alignment) {
        this.endX = x;
        this.endY = y;
        this.alignment = alignment;
    }

    public float getX() {
        return this.endX;
    }

    public void setX(float x) {
        this.endX = x;
    }

    public float getY() {
        return this.endY;
    }

    public void setY(float y) {
        this.endY = y;
    }

    public int getAlignment() {
        return this.alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }
}

