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

public class CardPoofParticle
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

    public CardPoofParticle(float x, float y) {
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
        this.startingDuration = this.duration = 0.6f;
        this.delay = MathUtils.random(0.0f, 0.1f);
        float t = MathUtils.random(-160.0f, 160.0f);
        this.x = x + t * Settings.scale - (float)this.img.packedWidth / 2.0f;
        t = MathUtils.random(-180.0f, 180.0f);
        this.y = y + t * Settings.scale - (float)this.img.packedHeight / 2.0f;
        float rg = MathUtils.random(0.4f, 0.8f);
        this.color = new Color(rg + 0.05f, rg, rg + 0.05f, 0.0f);
        this.vA = MathUtils.random(-400.0f, 400.0f) * Settings.scale;
        this.vX = MathUtils.random(-170.0f, 170.0f) * Settings.scale;
        this.vY = MathUtils.random(-170.0f, 170.0f) * Settings.scale;
        this.scale = MathUtils.random(0.8f, 2.5f) * Settings.scale;
        this.rotation = MathUtils.random(360.0f);
        this.renderBehind = true;
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
        this.scale += Gdx.graphics.getDeltaTime() * 5.0f;
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.pow3Out.apply(0.0f, 0.7f, 1.0f - this.duration) : Interpolation.fade.apply(0.0f, 0.7f, this.duration * 2.0f);
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

