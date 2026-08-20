/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class BlurWaveChaoticEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 2.0f;
    private float x;
    private float y;
    private float speed;
    private float speedStart;
    private float speedTarget;
    private float stallTimer;
    private TextureAtlas.AtlasRegion img = ImageMaster.BLUR_WAVE;
    private float flipper;

    public BlurWaveChaoticEffect(float x, float y, Color color, float chosenSpeed) {
        this.stallTimer = MathUtils.random(0.0f, 0.3f);
        this.rotation = MathUtils.random(360.0f);
        this.scale = MathUtils.random(0.5f, 0.9f);
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.duration = 2.0f;
        this.color = color;
        this.renderBehind = MathUtils.randomBoolean();
        this.speedStart = chosenSpeed;
        this.speedTarget = 2000.0f * Settings.scale;
        this.speed = this.speedStart;
        this.flipper = 270.0f;
        color.a = 0.0f;
    }

    @Override
    public void update() {
        this.stallTimer -= Gdx.graphics.getDeltaTime();
        if (this.stallTimer < 0.0f) {
            Vector2 tmp = new Vector2(MathUtils.cosDeg(this.rotation), MathUtils.sinDeg(this.rotation));
            tmp.x *= this.speed * Gdx.graphics.getDeltaTime();
            tmp.y *= this.speed * Gdx.graphics.getDeltaTime();
            this.speed = Interpolation.fade.apply(this.speedStart, this.speedTarget, 1.0f - this.duration / 2.0f);
            this.x += tmp.x;
            this.y += tmp.y;
            this.scale *= 1.0f + Gdx.graphics.getDeltaTime() * 2.0f;
            this.duration -= Gdx.graphics.getDeltaTime();
            if (this.duration < 0.0f) {
                this.isDone = true;
            } else if (this.duration > 1.5f) {
                this.color.a = Interpolation.fade.apply(0.0f, 0.7f, (2.0f - this.duration) * 2.0f);
            } else if (this.duration < 0.5f) {
                this.color.a = Interpolation.fade.apply(0.0f, 0.7f, this.duration * 2.0f);
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale + MathUtils.random(-0.1f, 0.1f), this.scale + MathUtils.random(-0.1f, 0.1f), this.rotation + this.flipper + MathUtils.random(-30.0f, 30.0f));
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

