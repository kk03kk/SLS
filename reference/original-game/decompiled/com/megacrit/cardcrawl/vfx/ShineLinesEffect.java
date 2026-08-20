/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ShineLinesEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 0.25f;
    private float x;
    private float y;
    private static final float SCALE_INCREASE_RATE = 8.0f;
    private TextureAtlas.AtlasRegion img = ImageMaster.GRAB_COIN;

    public ShineLinesEffect(float x, float y) {
        this.duration = 0.25f;
        this.startingDuration = 0.25f;
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.rotation = MathUtils.random(0.0f, 360.0f);
        this.color = Color.WHITE.cpy();
        this.scale = 0.0f;
    }

    @Override
    public void update() {
        super.update();
        this.scale += Gdx.graphics.getDeltaTime() * 8.0f;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            sb.setColor(this.color);
            sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        }
    }

    @Override
    public void dispose() {
    }
}

