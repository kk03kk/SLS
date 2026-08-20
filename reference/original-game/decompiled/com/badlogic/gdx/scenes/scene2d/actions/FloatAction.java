/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

public class FloatAction
extends TemporalAction {
    private float start;
    private float end;
    private float value;

    public FloatAction() {
        this.start = 0.0f;
        this.end = 1.0f;
    }

    public FloatAction(float start, float end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected void begin() {
        this.value = this.start;
    }

    @Override
    protected void update(float percent) {
        this.value = this.start + (this.end - this.start) * percent;
    }

    public float getValue() {
        return this.value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public float getStart() {
        return this.start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getEnd() {
        return this.end;
    }

    public void setEnd(float end) {
        this.end = end;
    }
}

