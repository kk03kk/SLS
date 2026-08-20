/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;

public class TextureRegionDrawable
extends BaseDrawable
implements TransformDrawable {
    private TextureRegion region;

    public TextureRegionDrawable() {
    }

    public TextureRegionDrawable(TextureRegion region) {
        this.setRegion(region);
    }

    public TextureRegionDrawable(TextureRegionDrawable drawable) {
        super(drawable);
        this.setRegion(drawable.region);
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        batch.draw(this.region, x, y, width, height);
    }

    @Override
    public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        batch.draw(this.region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
        this.setMinWidth(region.getRegionWidth());
        this.setMinHeight(region.getRegionHeight());
    }

    public TextureRegion getRegion() {
        return this.region;
    }

    public Drawable tint(Color tint) {
        Sprite sprite = this.region instanceof TextureAtlas.AtlasRegion ? new TextureAtlas.AtlasSprite((TextureAtlas.AtlasRegion)this.region) : new Sprite(this.region);
        sprite.setColor(tint);
        sprite.setSize(this.getMinWidth(), this.getMinHeight());
        SpriteDrawable drawable = new SpriteDrawable(sprite);
        drawable.setLeftWidth(this.getLeftWidth());
        drawable.setRightWidth(this.getRightWidth());
        drawable.setTopHeight(this.getTopHeight());
        drawable.setBottomHeight(this.getBottomHeight());
        return drawable;
    }
}

