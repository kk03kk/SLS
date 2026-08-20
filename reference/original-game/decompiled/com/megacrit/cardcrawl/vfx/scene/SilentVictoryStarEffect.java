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
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class SilentVictoryStarEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private TextureAtlas.AtlasRegion img;

    public SilentVictoryStarEffect() {
        this.startingDuration = this.duration = MathUtils.random(10.0f, 20.0f);
        this.renderBehind = true;
        if (MathUtils.randomBoolean()) {
            this.img = ImageMaster.ROOM_SHINE_1;
            this.rotation = MathUtils.random(-5.0f, 5.0f);
        } else {
            this.img = ImageMaster.GLOW_SPARK_2;
        }
        this.x = MathUtils.random(-100.0f, 1870.0f) * Settings.xScale - (float)this.img.packedWidth / 2.0f;
        float h = MathUtils.random(0.15f, 0.9f);
        this.y = (float)Settings.HEIGHT * h;
        this.vX = MathUtils.random(12.0f, 20.0f) * Settings.scale;
        this.vY = MathUtils.random(-5.0f, 5.0f) * Settings.scale;
        this.color = new Color(MathUtils.random(0.55f, 0.6f), MathUtils.random(0.8f, 1.0f), MathUtils.random(0.95f, 1.0f), 0.0f);
        this.scale = h * MathUtils.random(1.5f, 1.8f) * Settings.scale;
    }

    @Override
    public void update() {
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.fade.apply(0.9f, 0.0f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f)) : Interpolation.fade.apply(0.0f, 0.9f, this.duration / (this.startingDuration / 2.0f));
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.9f, 1.1f), this.scale * MathUtils.random(0.8f, 1.3f), this.rotation);
    }

    @Override
    public void dispose() {
    }
}

