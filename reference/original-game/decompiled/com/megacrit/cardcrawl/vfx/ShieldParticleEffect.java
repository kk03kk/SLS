/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ShieldParticleEffect
extends AbstractGameEffect {
    private static final int RAW_W = 64;
    private float x;
    private float y;
    private float scale = Settings.scale / 2.0f;

    public ShieldParticleEffect(float x, float y) {
        this.duration = 2.0f;
        this.x = x;
        this.y = y;
        this.renderBehind = true;
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    }

    @Override
    public void update() {
        this.scale += Gdx.graphics.getDeltaTime() * Settings.scale * 1.1f;
        this.color.a = this.duration > 1.0f ? Interpolation.fade.apply(0.0f, 0.3f, 1.0f - (this.duration - 1.0f)) : Interpolation.fade.apply(0.3f, 0.0f, 1.0f - this.duration);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(ImageMaster.INTENT_DEFEND, this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, 0.0f, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

