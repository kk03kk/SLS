/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.actions.RelativeTemporalAction;

public class SizeByAction
extends RelativeTemporalAction {
    private float amountWidth;
    private float amountHeight;

    @Override
    protected void updateRelative(float percentDelta) {
        this.target.sizeBy(this.amountWidth * percentDelta, this.amountHeight * percentDelta);
    }

    public void setAmount(float width, float height) {
        this.amountWidth = width;
        this.amountHeight = height;
    }

    public float getAmountWidth() {
        return this.amountWidth;
    }

    public void setAmountWidth(float width) {
        this.amountWidth = width;
    }

    public float getAmountHeight() {
        return this.amountHeight;
    }

    public void setAmountHeight(float height) {
        this.amountHeight = height;
    }
}

