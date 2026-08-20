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

public class FlameBallParticleEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float x;
    private float y;
    private float vY;

    public FlameBallParticleEffect(float x, float y, int intensity) {
        int roll = MathUtils.random(0, 2);
        this.img = roll == 0 ? ImageMaster.SMOKE_1 : (roll == 1 ? ImageMaster.SMOKE_2 : ImageMaster.SMOKE_3);
        this.scale = (1.0f + (float)intensity * 0.1f) * Settings.scale;
        this.duration = MathUtils.random(1.0f, 1.6f);
        this.x = x - (float)(this.img.packedWidth / 2);
        this.y = y - (float)(this.img.packedHeight / 2) + (float)intensity * 3.0f * Settings.scale;
        this.color = new Color(MathUtils.random(0.8f, 1.0f), MathUtils.random(0.2f, 0.8f), MathUtils.random(0.0f, 0.4f), 0.0f);
        this.color.a = 0.0f;
        this.rotation = MathUtils.random(-5.0f, 5.0f);
        this.vY = MathUtils.random(10.0f, 30.0f) * Settings.scale;
        this.renderBehind = MathUtils.randomBoolean();
        this.startingDuration = 1.0f;
    }

    @Override
    public void update() {
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.fade.apply(0.7f, 0.0f, this.duration - this.startingDuration / 2.0f) * Settings.scale : Interpolation.fade.apply(0.0f, 0.7f, this.duration / (this.startingDuration / 2.0f)) * Settings.scale;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f + 20.0f * Settings.scale, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

