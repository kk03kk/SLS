/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

public class RotateToAction
extends TemporalAction {
    private float start;
    private float end;

    @Override
    protected void begin() {
        this.start = this.target.getRotation();
    }

    @Override
    protected void update(float percent) {
        this.target.setRotation(this.start + (this.end - this.start) * percent);
    }

    public float getRotation() {
        return this.end;
    }

    public void setRotation(float rotation) {
        this.end = rotation;
    }
}

