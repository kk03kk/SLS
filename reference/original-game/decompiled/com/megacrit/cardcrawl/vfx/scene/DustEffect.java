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
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DustEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float aV;
    private float baseAlpha;
    private TextureAtlas.AtlasRegion img;

    public DustEffect() {
        this.duration = this.startingDuration = MathUtils.random(5.0f, 14.0f);
        this.img = this.getImg();
        this.scale = Settings.scale * MathUtils.random(0.1f, 0.8f);
        this.x = MathUtils.random(0, Settings.WIDTH);
        this.y = MathUtils.random(-100.0f, 400.0f) * Settings.scale + AbstractDungeon.floorY;
        this.vX = MathUtils.random(-12.0f, 12.0f) * Settings.scale;
        this.vY = MathUtils.random(-12.0f, 30.0f) * Settings.scale;
        float colorTmp = MathUtils.random(0.1f, 0.7f);
        this.color = new Color(colorTmp, colorTmp, colorTmp, 0.0f);
        this.baseAlpha = 1.0f - colorTmp;
        this.color.a = 0.0f;
        this.rotation = MathUtils.random(0.0f, 360.0f);
        this.aV = MathUtils.random(-120.0f, 120.0f);
    }

    private TextureAtlas.AtlasRegion getImg() {
        switch (MathUtils.random(0, 5)) {
            case 0: {
                return ImageMaster.DUST_1;
            }
            case 1: {
                return ImageMaster.DUST_2;
            }
            case 2: {
                return ImageMaster.DUST_3;
            }
            case 3: {
                return ImageMaster.DUST_4;
            }
            case 4: {
                return ImageMaster.DUST_5;
            }
        }
        return ImageMaster.DUST_6;
    }

    @Override
    public void update() {
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration > this.startingDuration / 2.0f) {
            float tmp = this.duration - this.startingDuration / 2.0f;
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.startingDuration / 2.0f - tmp) * this.baseAlpha;
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration / (this.startingDuration / 2.0f)) * this.baseAlpha;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, this.img.offsetX, this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}

