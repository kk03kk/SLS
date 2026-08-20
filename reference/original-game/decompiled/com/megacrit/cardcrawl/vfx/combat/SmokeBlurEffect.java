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
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class SmokeBlurEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private float aV;
    private float startDur;
    private float targetScale;
    private TextureAtlas.AtlasRegion img;

    public SmokeBlurEffect(float x, float y) {
        this.color = new Color(0.0f, 0.0f, 0.0f, 1.0f);
        this.color.r = MathUtils.random(0.5f, 0.6f);
        this.color.g = this.color.r + MathUtils.random(0.0f, 0.2f);
        this.color.b = 0.2f;
        if (MathUtils.randomBoolean()) {
            this.img = ImageMaster.EXHAUST_L;
            this.duration = MathUtils.random(2.0f, 2.5f);
            this.targetScale = MathUtils.random(0.8f, 2.2f);
        } else {
            this.img = ImageMaster.EXHAUST_S;
            this.duration = MathUtils.random(2.0f, 2.5f);
            this.targetScale = MathUtils.random(0.8f, 1.2f);
        }
        this.startDur = this.duration;
        this.x = x + MathUtils.random(-180.0f * Settings.scale, 150.0f * Settings.scale) - (float)this.img.packedWidth / 2.0f;
        this.y = y + MathUtils.random(-240.0f * Settings.scale, 150.0f * Settings.scale) - (float)this.img.packedHeight / 2.0f;
        this.scale = 0.01f;
        this.rotation = MathUtils.random(360.0f);
        this.aV = MathUtils.random(-250.0f, 250.0f);
        this.vY = MathUtils.random(1.0f * Settings.scale, 5.0f * Settings.scale);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.x += MathUtils.random(-2.0f * Settings.scale, 2.0f * Settings.scale);
        this.x += this.vY;
        this.y += MathUtils.random(-2.0f * Settings.scale, 2.0f * Settings.scale);
        this.y += this.vY;
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        this.scale = Interpolation.exp10Out.apply(0.01f, this.targetScale, 1.0f - this.duration / this.startDur);
        if (this.duration < 0.33f) {
            this.color.a = this.duration * 3.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}

