/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;
import com.badlogic.gdx.utils.Scaling;

public class Image
extends Widget {
    private Scaling scaling;
    private int align = 1;
    private float imageX;
    private float imageY;
    private float imageWidth;
    private float imageHeight;
    private Drawable drawable;

    public Image() {
        this((Drawable)null);
    }

    public Image(NinePatch patch) {
        this(new NinePatchDrawable(patch), Scaling.stretch, 1);
    }

    public Image(TextureRegion region) {
        this(new TextureRegionDrawable(region), Scaling.stretch, 1);
    }

    public Image(Texture texture) {
        this(new TextureRegionDrawable(new TextureRegion(texture)));
    }

    public Image(Skin skin, String drawableName) {
        this(skin.getDrawable(drawableName), Scaling.stretch, 1);
    }

    public Image(Drawable drawable) {
        this(drawable, Scaling.stretch, 1);
    }

    public Image(Drawable drawable, Scaling scaling) {
        this(drawable, scaling, 1);
    }

    public Image(Drawable drawable, Scaling scaling, int align) {
        this.setDrawable(drawable);
        this.scaling = scaling;
        this.align = align;
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
    }

    @Override
    public void layout() {
        if (this.drawable == null) {
            return;
        }
        float regionWidth = this.drawable.getMinWidth();
        float regionHeight = this.drawable.getMinHeight();
        float width = this.getWidth();
        float height = this.getHeight();
        Vector2 size = this.scaling.apply(regionWidth, regionHeight, width, height);
        this.imageWidth = size.x;
        this.imageHeight = size.y;
        this.imageX = (this.align & 8) != 0 ? 0.0f : ((this.align & 0x10) != 0 ? (float)((int)(width - this.imageWidth)) : (float)((int)(width / 2.0f - this.imageWidth / 2.0f)));
        this.imageY = (this.align & 2) != 0 ? (float)((int)(height - this.imageHeight)) : ((this.align & 4) != 0 ? 0.0f : (float)((int)(height / 2.0f - this.imageHeight / 2.0f)));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        this.validate();
        Color color = this.getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        float x = this.getX();
        float y = this.getY();
        float scaleX = this.getScaleX();
        float scaleY = this.getScaleY();
        if (this.drawable instanceof TransformDrawable) {
            float rotation = this.getRotation();
            if (scaleX != 1.0f || scaleY != 1.0f || rotation != 0.0f) {
                ((TransformDrawable)this.drawable).draw(batch, x + this.imageX, y + this.imageY, this.getOriginX() - this.imageX, this.getOriginY() - this.imageY, this.imageWidth, this.imageHeight, scaleX, scaleY, rotation);
                return;
            }
        }
        if (this.drawable != null) {
            this.drawable.draw(batch, x + this.imageX, y + this.imageY, this.imageWidth * scaleX, this.imageHeight * scaleY);
        }
    }

    public void setDrawable(Skin skin, String drawableName) {
        this.setDrawable(skin.getDrawable(drawableName));
    }

    public void setDrawable(Drawable drawable) {
        if (this.drawable == drawable) {
            return;
        }
        if (drawable != null) {
            if (this.getPrefWidth() != drawable.getMinWidth() || this.getPrefHeight() != drawable.getMinHeight()) {
                this.invalidateHierarchy();
            }
        } else {
            this.invalidateHierarchy();
        }
        this.drawable = drawable;
    }

    public Drawable getDrawable() {
        return this.drawable;
    }

    public void setScaling(Scaling scaling) {
        if (scaling == null) {
            throw new IllegalArgumentException("scaling cannot be null.");
        }
        this.scaling = scaling;
        this.invalidate();
    }

    public void setAlign(int align) {
        this.align = align;
        this.invalidate();
    }

    @Override
    public float getMinWidth() {
        return 0.0f;
    }

    @Override
    public float getMinHeight() {
        return 0.0f;
    }

    @Override
    public float getPrefWidth() {
        if (this.drawable != null) {
            return this.drawable.getMinWidth();
        }
        return 0.0f;
    }

    @Override
    public float getPrefHeight() {
        if (this.drawable != null) {
            return this.drawable.getMinHeight();
        }
        return 0.0f;
    }

    public float getImageX() {
        return this.imageX;
    }

    public float getImageY() {
        return this.imageY;
    }

    public float getImageWidth() {
        return this.imageWidth;
    }

    public float getImageHeight() {
        return this.imageHeight;
    }
}

