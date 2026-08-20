/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FallingDustEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private float vX;
    private float vYAccel;
    private float aV;
    private float startingAlpha;
    private TextureAtlas.AtlasRegion img;

    public FallingDustEffect(float x, float y) {
        this.x = x + MathUtils.random(-6.0f, 6.0f) * Settings.scale;
        this.y = y;
        float randY = MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        y += randY;
        this.renderBehind = randY < 0.0f;
        this.vY = MathUtils.random(0.0f, 140.0f) * Settings.scale;
        this.vX = MathUtils.randomBoolean() ? MathUtils.random(-20.0f, 20.0f) * Settings.scale : 0.0f;
        this.vYAccel = MathUtils.random(4.0f, 9.0f) * Settings.scale;
        this.duration = MathUtils.random(3.0f, 7.0f);
        this.img = this.setImg();
        this.scale = Settings.scale * MathUtils.random(0.5f, 0.7f);
        this.rotation = MathUtils.random(0.0f, 360.0f);
        float c = MathUtils.random(0.1f, 0.3f);
        this.color = new Color(c + 0.1f, c, c, 0.0f);
        this.startingAlpha = this.color.a = MathUtils.random(0.8f, 0.9f);
        this.aV = MathUtils.random(-1.0f, 1.0f);
    }

    private TextureAtlas.AtlasRegion setImg() {
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
        this.rotation += this.aV;
        this.y -= this.vY * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.vY += this.vYAccel * Gdx.graphics.getDeltaTime();
        this.vX *= 0.99f;
        if (this.duration < 3.0f) {
            this.color.a = Interpolation.fade.apply(this.startingAlpha, 0.0f, 1.0f - this.duration / 3.0f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
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

