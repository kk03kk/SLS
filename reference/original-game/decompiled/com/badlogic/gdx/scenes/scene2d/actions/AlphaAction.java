/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

public class AlphaAction
extends TemporalAction {
    private float start;
    private float end;
    private Color color;

    @Override
    protected void begin() {
        if (this.color == null) {
            this.color = this.target.getColor();
        }
        this.start = this.color.a;
    }

    @Override
    protected void update(float percent) {
        this.color.a = this.start + (this.end - this.start) * percent;
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

    public float getAlpha() {
        return this.end;
    }

    public void setAlpha(float alpha) {
        this.end = alpha;
    }
}

