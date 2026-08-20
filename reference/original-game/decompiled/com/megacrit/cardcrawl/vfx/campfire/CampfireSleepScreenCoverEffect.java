/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.campfire;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CampfireSleepScreenCoverEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float aV;
    private float targetAlpha = MathUtils.random(0.4f, 0.7f);
    private boolean flipX = MathUtils.randomBoolean();
    private boolean flipY = MathUtils.randomBoolean();
    private TextureAtlas.AtlasRegion img;

    public CampfireSleepScreenCoverEffect() {
        this.startingDuration = this.duration = MathUtils.random(2.0f, 2.5f);
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
        this.x = MathUtils.random(-100.0f * Settings.scale, (float)Settings.WIDTH + 100.0f * Settings.scale) - (float)this.img.packedWidth / 2.0f;
        this.y = MathUtils.random(-100.0f * Settings.scale, (float)Settings.HEIGHT + 100.0f * Settings.scale) - (float)this.img.packedHeight / 2.0f;
        this.aV = MathUtils.random(-30.0f, 30.0f);
        this.rotation = MathUtils.random(0.0f, 360.0f);
        float tmp = MathUtils.random(0.5f, 0.7f);
        this.color = new Color();
        this.color.r = tmp;
        this.color.g = tmp - 0.03f;
        this.color.b = tmp - 0.07f;
        this.scale = MathUtils.random(16.0f, 30.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        if (this.startingDuration - this.duration < 1.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, this.targetAlpha, this.startingDuration - this.duration);
        } else if (this.duration < 1.0f) {
            this.color.a = Interpolation.fade.apply(this.targetAlpha, 0.0f, 1.0f - this.duration);
        }
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

