/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class SlowFireParticleEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float x;
    private float y;
    private float vX;
    private float vY2;
    private float startingDuration;

    public SlowFireParticleEffect() {
        this.setImg();
        this.renderBehind = true;
        this.duration = this.startingDuration = 2.0f;
        this.x = MathUtils.random(0.0f, (float)Settings.WIDTH) - (float)this.img.packedWidth / 2.0f;
        this.y = (float)(-this.img.packedHeight) / 2.0f - 100.0f * Settings.scale;
        this.vX = MathUtils.random(-120.0f, 120.0f) * Settings.scale;
        this.vY2 = MathUtils.random(5.0f, 30.0f);
        this.vY2 *= this.vY2;
        this.vY2 *= Settings.scale;
        this.color = new Color(MathUtils.random(0.3f, 0.4f), MathUtils.random(0.3f, 0.7f), MathUtils.random(0.8f, 1.0f), 0.0f);
        this.rotation = this.vX > 0.0f ? MathUtils.random(0.0f, -15.0f) : MathUtils.random(0.0f, 15.0f);
        this.scale = MathUtils.random(0.3f, 3.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY2 * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else {
            this.color.a = Interpolation.pow2Out.apply(0.0f, 0.7f, this.duration);
        }
    }

    private void setImg() {
        this.img = MathUtils.randomBoolean() ? ImageMaster.GLOW_SPARK_2 : ImageMaster.GLOW_SPARK;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
    }

    @Override
    public void dispose() {
    }
}

