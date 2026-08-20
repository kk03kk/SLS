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

public class DeckPoofParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float vA;
    private float delay;
    private float scale = Settings.scale;
    private boolean flipX = MathUtils.randomBoolean();
    private boolean flipY = MathUtils.randomBoolean();
    private TextureAtlas.AtlasRegion img;

    public DeckPoofParticle(float x, float y, boolean isDeck) {
        switch (MathUtils.random(2)) {
            case 0: {
                this.img = ImageMaster.SMOKE_1;
                break;
            }
            case 1: {
                this.img = ImageMaster.SMOKE_2;
                break;
            }
            default: {
                this.img = ImageMaster.SMOKE_3;
            }
        }
        this.startingDuration = this.duration = 0.8f;
        this.delay = MathUtils.random(0.0f, 0.2f);
        float t = MathUtils.random(-10.0f, 10.0f) * MathUtils.random(-10.0f, 10.0f);
        this.x = x + t * Settings.scale - (float)this.img.packedWidth / 2.0f;
        t = MathUtils.random(-10.0f, 10.0f) * MathUtils.random(-10.0f, 10.0f);
        this.y = y + t * Settings.scale - (float)this.img.packedHeight / 2.0f;
        if (isDeck) {
            float rg = MathUtils.random(0.4f, 0.8f);
            this.color = new Color(rg + 0.1f, rg, rg - 0.2f, 0.0f);
            this.vA = MathUtils.random(-400.0f, 400.0f) * Settings.scale;
        } else {
            float rb = MathUtils.random(0.3f, 0.5f);
            this.color = new Color(rb, 0.35f, rb + 0.1f, 0.0f);
            this.vA = MathUtils.random(-70.0f, 70.0f) * MathUtils.random(-70.0f, 70.0f) * Settings.scale;
        }
        this.vX = MathUtils.random(-70.0f, 70.0f) * Settings.scale;
        this.vY = MathUtils.random(-100.0f, 300.0f) * Settings.scale;
        this.scale = MathUtils.random(0.3f, 1.8f) * Settings.scale;
        this.rotation = MathUtils.random(360.0f);
    }

    @Override
    public void update() {
        if (this.delay > 0.0f) {
            this.delay -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.rotation += this.vA * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vY *= 0.99f;
        this.vX *= 0.99f;
        this.scale += Gdx.graphics.getDeltaTime() / 2.0f;
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.pow3Out.apply(0.0f, 1.0f, 1.0f - this.duration) : Interpolation.fade.apply(0.0f, 1.0f, this.duration * 2.0f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        if (this.flipX && !this.img.isFlipX()) {
            this.img.flip(true, false);
        } else if (!this.flipX && this.img.isFlipX()) {
            this.img.flip(true, false);
        }
        if (this.flipY && !this.img.isFlipY()) {
            this.img.flip(false, true);
        } else if (!this.flipY && this.img.isFlipY()) {
            this.img.flip(false, true);
        }
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}

