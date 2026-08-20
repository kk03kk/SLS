/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;

public class SpriteDrawable
extends BaseDrawable
implements TransformDrawable {
    private Sprite sprite;

    public SpriteDrawable() {
    }

    public SpriteDrawable(Sprite sprite) {
        this.setSprite(sprite);
    }

    public SpriteDrawable(SpriteDrawable drawable) {
        super(drawable);
        this.setSprite(drawable.sprite);
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        Color spriteColor = this.sprite.getColor();
        float batchColor = batch.getPackedColor();
        this.sprite.setColor(batch.getColor().mul(spriteColor));
        this.sprite.setRotation(0.0f);
        this.sprite.setScale(1.0f, 1.0f);
        this.sprite.setBounds(x, y, width, height);
        this.sprite.draw(batch);
        this.sprite.setColor(spriteColor);
        batch.setColor(batchColor);
    }

    @Override
    public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        Color spriteColor = this.sprite.getColor();
        float batchColor = batch.getPackedColor();
        this.sprite.setColor(batch.getColor().mul(spriteColor));
        this.sprite.setOrigin(originX, originY);
        this.sprite.setRotation(rotation);
        this.sprite.setScale(scaleX, scaleY);
        this.sprite.setBounds(x, y, width, height);
        this.sprite.draw(batch);
        this.sprite.setColor(spriteColor);
        batch.setColor(batchColor);
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
        this.setMinWidth(sprite.getWidth());
        this.setMinHeight(sprite.getHeight());
    }

    public Sprite getSprite() {
        return this.sprite;
    }

    public SpriteDrawable tint(Color tint) {
        Sprite newSprite = this.sprite instanceof TextureAtlas.AtlasSprite ? new TextureAtlas.AtlasSprite((TextureAtlas.AtlasSprite)this.sprite) : new Sprite(this.sprite);
        newSprite.setColor(tint);
        newSprite.setSize(this.getMinWidth(), this.getMinHeight());
        SpriteDrawable drawable = new SpriteDrawable(newSprite);
        drawable.setLeftWidth(this.getLeftWidth());
        drawable.setRightWidth(this.getRightWidth());
        drawable.setTopHeight(this.getTopHeight());
        drawable.setBottomHeight(this.getBottomHeight());
        return drawable;
    }
}

