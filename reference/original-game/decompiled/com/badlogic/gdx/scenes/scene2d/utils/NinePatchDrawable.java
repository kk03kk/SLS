/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;

public class NinePatchDrawable
extends BaseDrawable
implements TransformDrawable {
    private NinePatch patch;

    public NinePatchDrawable() {
    }

    public NinePatchDrawable(NinePatch patch) {
        this.setPatch(patch);
    }

    public NinePatchDrawable(NinePatchDrawable drawable) {
        super(drawable);
        this.setPatch(drawable.patch);
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        this.patch.draw(batch, x, y, width, height);
    }

    @Override
    public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        this.patch.draw(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

    public void setPatch(NinePatch patch) {
        this.patch = patch;
        this.setMinWidth(patch.getTotalWidth());
        this.setMinHeight(patch.getTotalHeight());
        this.setTopHeight(patch.getPadTop());
        this.setRightWidth(patch.getPadRight());
        this.setBottomHeight(patch.getPadBottom());
        this.setLeftWidth(patch.getPadLeft());
    }

    public NinePatch getPatch() {
        return this.patch;
    }

    public NinePatchDrawable tint(Color tint) {
        NinePatchDrawable drawable = new NinePatchDrawable(this);
        drawable.setPatch(new NinePatch(drawable.getPatch(), tint));
        return drawable;
    }
}

