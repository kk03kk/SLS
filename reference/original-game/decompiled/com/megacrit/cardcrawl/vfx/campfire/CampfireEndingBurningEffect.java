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
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CampfireEndingBurningEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float x;
    private float y;
    private float vX;
    private float vY2;
    private float vY;
    private float startingDuration;
    private boolean flipX = MathUtils.randomBoolean();
    private float delayTimer = MathUtils.random(0.1f);

    public CampfireEndingBurningEffect() {
        this.setImg();
        this.duration = this.startingDuration = 1.0f;
        this.x = MathUtils.random(0.0f, (float)Settings.WIDTH) - (float)this.img.packedWidth / 2.0f;
        this.y = (float)(-this.img.packedHeight) / 2.0f - 100.0f * Settings.scale;
        this.vX = MathUtils.random(-120.0f, 120.0f) * Settings.scale;
        this.vY = 0.0f;
        this.vY2 = MathUtils.random(1500.0f, 3000.0f) * Settings.scale;
        this.vY2 -= Math.abs(this.x - 1485.0f * Settings.scale) / 2.0f;
        this.color = new Color(1.0f, MathUtils.random(0.5f, 0.9f), MathUtils.random(0.2f, 0.5f), 0.0f);
        this.rotation = this.vX > 0.0f ? MathUtils.random(0.0f, -15.0f) : MathUtils.random(0.0f, 15.0f);
        this.scale = MathUtils.random(1.5f, 4.0f) * Settings.scale;
    }

    @Override
    public void update() {
        if (this.delayTimer > 0.0f) {
            this.delayTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vY = MathHelper.slowColorLerpSnap(this.vY, this.vY2);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else {
            this.color.a = Interpolation.pow2Out.apply(0.0f, 0.8f, this.duration);
        }
    }

    private void setImg() {
        int roll = MathUtils.random(2);
        this.img = roll == 0 ? ImageMaster.GLOW_SPARK : (roll == 1 ? ImageMaster.GLOW_SPARK : ImageMaster.GLOW_SPARK_2);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        if (this.flipX && !this.img.isFlipX()) {
            this.img.flip(true, false);
        } else if (!this.flipX && this.img.isFlipX()) {
            this.img.flip(true, false);
        }
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

