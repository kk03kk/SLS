/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

public class ColorAction
extends TemporalAction {
    private float startR;
    private float startG;
    private float startB;
    private float startA;
    private Color color;
    private final Color end = new Color();

    @Override
    protected void begin() {
        if (this.color == null) {
            this.color = this.target.getColor();
        }
        this.startR = this.color.r;
        this.startG = this.color.g;
        this.startB = this.color.b;
        this.startA = this.color.a;
    }

    @Override
    protected void update(float percent) {
        float r = this.startR + (this.end.r - this.startR) * percent;
        float g = this.startG + (this.end.g - this.startG) * percent;
        float b = this.startB + (this.end.b - this.startB) * percent;
        float a = this.startA + (this.end.a - this.startA) * percent;
        this.color.set(r, g, b, a);
    }

    @Override
    public void reset() {
        super.reset();
        this.color = null;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getEndColor() {
        return this.end;
    }

    public void setEndColor(Color color) {
        this.end.set(color);
    }
}

