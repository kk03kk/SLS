/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class GhostlyWeakFireEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img = this.getImg();
    private float x;
    private float y;
    private float vX;
    private float vY;
    private static final float DUR = 1.0f;

    public GhostlyWeakFireEffect(float x, float y) {
        this.x = x + MathUtils.random(-2.0f, 2.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = y + MathUtils.random(-2.0f, 2.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-2.0f, 2.0f) * Settings.scale;
        this.vY = MathUtils.random(0.0f, 80.0f) * Settings.scale;
        this.duration = 1.0f;
        this.color = Color.SKY.cpy();
        this.color.a = 0.0f;
        this.scale = Settings.scale * MathUtils.random(2.0f, 3.0f);
    }

    private TextureAtlas.AtlasRegion getImg() {
        switch (MathUtils.random(0, 2)) {
            case 0: {
                return ImageMaster.TORCH_FIRE_1;
            }
            case 1: {
                return ImageMaster.TORCH_FIRE_2;
            }
        }
        return ImageMaster.TORCH_FIRE_3;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = this.duration / 2.0f;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

