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

public class GiantFireEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float brightness;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float startingDuration;
    private boolean flipX = MathUtils.randomBoolean();
    private float delayTimer = MathUtils.random(0.1f);

    public GiantFireEffect() {
        this.setImg();
        this.duration = this.startingDuration = 1.5f;
        this.x = MathUtils.random(0.0f, (float)Settings.WIDTH) - (float)this.img.packedWidth / 2.0f;
        this.y = MathUtils.random(-200.0f, -400.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-70.0f, 70.0f) * Settings.scale;
        this.vY = MathUtils.random(500.0f, 1700.0f) * Settings.scale;
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
        this.color.g -= MathUtils.random(0.5f);
        this.color.b -= this.color.g - MathUtils.random(0.0f, 0.2f);
        this.rotation = MathUtils.random(-10.0f, 10.0f);
        this.scale = MathUtils.random(0.5f, 7.0f);
        this.brightness = MathUtils.random(0.2f, 0.6f);
    }

    @Override
    public void update() {
        if (this.delayTimer > 0.0f) {
            this.delayTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.scale *= MathUtils.random(0.95f, 1.05f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.startingDuration - this.duration < 0.75f) {
            this.color.a = Interpolation.fade.apply(0.0f, this.brightness, (this.startingDuration - this.duration) / 0.75f);
        } else if (this.duration < 1.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, this.brightness, this.duration / 1.0f);
        }
    }

    private void setImg() {
        int roll = MathUtils.random(2);
        this.img = roll == 0 ? ImageMaster.FLAME_1 : (roll == 1 ? ImageMaster.FLAME_2 : ImageMaster.FLAME_3);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        if (this.flipX && !this.img.isFlipX()) {
            this.img.flip(true, false);
        } else if (!this.flipX && this.img.isFlipX()) {
            this.img.flip(true, false);
        }
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * Settings.scale, this.scale * Settings.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

