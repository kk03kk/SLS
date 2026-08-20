/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.actions.DelegateAction;

public class TimeScaleAction
extends DelegateAction {
    private float scale;

    @Override
    protected boolean delegate(float delta) {
        if (this.action == null) {
            return true;
        }
        return this.action.act(delta * this.scale);
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}

