/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FastSmokeParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float scale = 0.01f;
    private float targetScale;
    private static TextureAtlas.AtlasRegion img;

    public FastSmokeParticle(float x, float y) {
        if (img == null) {
            img = ImageMaster.EXHAUST_L;
        }
        this.targetScale = MathUtils.random(0.3f, 0.6f) * Settings.scale;
        this.color = new Color(1.0f, MathUtils.random(0.8f, 1.0f), MathUtils.random(0.5f, 0.8f), 1.0f);
        this.x = x - (float)FastSmokeParticle.img.packedWidth / 2.0f;
        this.y = y - (float)FastSmokeParticle.img.packedHeight / 2.0f;
        this.rotation = MathUtils.random(360.0f);
        this.duration = 0.6f;
    }

    @Override
    public void update() {
        if (this.color.b > 0.1f) {
            this.color.b -= Gdx.graphics.getDeltaTime() * 4.0f;
            this.color.g -= Gdx.graphics.getDeltaTime() * 3.0f;
            this.color.r -= Gdx.graphics.getDeltaTime() * 0.5f;
        } else if (this.color.g > 0.1f) {
            this.color.g -= Gdx.graphics.getDeltaTime() * 4.0f;
        } else if (this.color.r > 0.1f) {
            this.color.r -= Gdx.graphics.getDeltaTime() * 4.0f;
        }
        if (this.color.b < 0.0f) {
            this.color.b = 0.0f;
        }
        if (this.color.g < 0.0f) {
            this.color.g = 0.0f;
        }
        if (this.color.r < 0.0f) {
            this.color.r = 0.0f;
        }
        this.scale = Interpolation.swingIn.apply(this.targetScale, 0.1f, this.duration / 0.6f);
        this.rotation += this.vX * 2.0f * Gdx.graphics.getDeltaTime();
        this.color.a = this.duration;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y, (float)FastSmokeParticle.img.packedWidth / 2.0f, (float)FastSmokeParticle.img.packedHeight / 2.0f, FastSmokeParticle.img.packedWidth, FastSmokeParticle.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}

