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
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CampfireBurningEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float brightness;
    private float x;
    private float y;
    private float vX;
    private float vY2;
    private float vY;
    private float startingDuration;
    private boolean flipX = MathUtils.randomBoolean();
    private float delayTimer = MathUtils.random(0.1f);

    public CampfireBurningEffect() {
        this.setImg();
        this.duration = this.startingDuration = 1.75f;
        this.x = MathUtils.random(1200.0f, 1770.0f) * Settings.xScale - (float)this.img.packedWidth / 2.0f;
        this.y = (float)Settings.HEIGHT / 2.0f - 200.0f * Settings.yScale - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-70.0f, 70.0f) * Settings.xScale;
        this.vY = 0.0f;
        this.vY2 = MathUtils.random(250.0f, 450.0f) * Settings.scale;
        this.vY2 -= Math.abs(this.x - 1485.0f * Settings.scale) / 2.0f;
        if (CardCrawlGame.dungeon instanceof TheBeyond) {
            this.color = new Color(0.0f, 1.0f, 1.0f, 0.0f);
            this.color.g -= MathUtils.random(0.4f);
            this.color.b -= MathUtils.random(0.4f);
        } else if (CardCrawlGame.dungeon instanceof TheCity) {
            this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
            this.color.r -= MathUtils.random(0.5f);
            this.color.b -= this.color.r - MathUtils.random(0.0f, 0.2f);
        } else {
            this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
            this.color.g -= MathUtils.random(0.5f);
            this.color.b -= this.color.g - MathUtils.random(0.0f, 0.2f);
        }
        this.rotation = MathUtils.random(-10.0f, 10.0f);
        this.scale = MathUtils.random(3.0f, 4.0f);
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
        this.vY = MathHelper.slowColorLerpSnap(this.vY, this.vY2);
        this.scale *= 1.0f - Gdx.graphics.getDeltaTime() / 10.0f;
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
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
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

