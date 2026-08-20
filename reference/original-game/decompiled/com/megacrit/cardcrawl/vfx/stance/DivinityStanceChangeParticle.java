/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.stance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DivinityStanceChangeParticle
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img = ImageMaster.STRIKE_LINE;
    private float oX;
    private float oY;
    private float x;
    private float y;
    private float aV;
    private float distOffset;
    private float scaleOffset;

    public DivinityStanceChangeParticle(Color color, float x, float y) {
        this.duration = this.startingDuration = 0.5f;
        this.color = color.cpy();
        this.rotation = MathUtils.random(360.0f);
        this.oX = x - (float)this.img.packedWidth / 2.0f + MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.oY = y - (float)this.img.packedHeight / 2.0f + MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.distOffset = MathUtils.random(800.0f, 1200.0f);
        this.renderBehind = true;
        this.aV = MathUtils.random(50.0f, 80.0f);
        this.scaleOffset = MathUtils.random(4.0f, 5.0f);
        this.aV = MathUtils.random(0.4f);
    }

    @Override
    public void update() {
        if (this.aV > 0.0f) {
            this.aV -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            return;
        }
        this.x = this.oX + MathUtils.cosDeg(this.rotation) * this.distOffset * Interpolation.pow2In.apply(0.02f, 0.95f, this.duration * 2.0f) * Settings.scale;
        this.y = this.oY + MathUtils.sinDeg(this.rotation) * this.distOffset * Interpolation.pow3In.apply(0.02f, 0.95f, this.duration * 2.0f) * Settings.scale;
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale = this.scaleOffset * (this.duration + 0.1f) * Settings.scale;
        this.color.a = Interpolation.pow3In.apply(0.0f, 1.0f, this.duration * 2.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

