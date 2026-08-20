/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class WebLineEffect
extends AbstractGameEffect {
    float x;
    float y;

    public WebLineEffect(float x, float y, boolean facingLeft) {
        this.x = x + MathUtils.random(-20.0f, 20.0f) * Settings.scale;
        this.y = y - 128.0f + MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.duration = this.startingDuration = 1.0f;
        this.rotation = MathUtils.random(185.0f, 170.0f);
        if (!facingLeft) {
            this.rotation += 180.0f;
        }
        this.scale = MathUtils.random(0.8f, 1.0f) * Settings.scale;
        float g = MathUtils.random(0.6f, 0.9f);
        this.color = new Color(g, g, g + 0.1f, 0.0f);
        this.renderBehind = false;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.fade.apply(0.8f, 0.01f, this.duration - this.startingDuration / 2.0f) * Settings.scale : Interpolation.pow5Out.apply(0.01f, 0.8f, this.duration / (this.startingDuration / 2.0f)) * Settings.scale;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.color.a));
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.HORIZONTAL_LINE, this.x, this.y, 0.0f, 128.0f, 256.0f, 256.0f, this.scale * 2.0f * (MathUtils.cos(this.duration * 16.0f) / 4.0f + 1.5f), this.scale, this.rotation, 0, 0, 256, 256, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

