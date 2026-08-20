/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.stance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class StanceChangeAbsorptionParticle
extends AbstractGameEffect {
    private float oX;
    private float oY;
    private float x;
    private float y;
    private float aV;
    private float distOffset;
    private float scaleOffset;

    public StanceChangeAbsorptionParticle(Color color, float x, float y) {
        this.duration = this.startingDuration = 1.0f;
        this.color = color.cpy();
        this.color.r -= MathUtils.random(0.1f);
        this.color.g -= MathUtils.random(0.1f);
        this.color.b -= MathUtils.random(0.1f);
        this.rotation = MathUtils.random(360.0f);
        this.oX = x;
        this.oY = y;
        this.distOffset = MathUtils.random(-200.0f, 1000.0f);
        this.renderBehind = true;
        this.aV = MathUtils.random(50.0f, 80.0f);
        this.scaleOffset = MathUtils.random(1.0f);
    }

    @Override
    public void update() {
        this.x = this.oX + MathUtils.cosDeg(this.rotation) * (800.0f + this.distOffset) * this.duration * Settings.scale;
        this.y = this.oY + MathUtils.sinDeg(this.rotation) * (800.0f + this.distOffset) * this.duration * Settings.scale;
        this.duration -= Gdx.graphics.getDeltaTime();
        this.rotation -= this.duration * Interpolation.pow5Out.apply(this.aV, 2.0f, this.duration);
        this.scale = (this.duration + 0.2f + this.scaleOffset) * Settings.scale;
        this.color.a = Interpolation.pow2Out.apply(0.0f, 1.0f, this.duration);
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.WOBBLY_ORB_VFX, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * MathUtils.random(0.5f, 2.0f), this.scale * MathUtils.random(0.5f, 2.0f), this.rotation - 200.0f, 0, 0, 32, 32, false, false);
        sb.draw(ImageMaster.WOBBLY_ORB_VFX, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * MathUtils.random(0.6f, 2.5f), this.scale * MathUtils.random(0.6f, 2.5f), this.rotation - 200.0f, 0, 0, 32, 32, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

