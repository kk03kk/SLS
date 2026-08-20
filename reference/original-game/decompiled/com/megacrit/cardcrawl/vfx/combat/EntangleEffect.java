/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class EntangleEffect
extends AbstractGameEffect {
    float x;
    float y;
    float dX;
    float dY;
    float tX;
    float tY;

    public EntangleEffect(float x, float y, float dX, float dY) {
        this.tX = x - 32.0f;
        this.tY = y - 32.0f;
        this.dX = dX - 32.0f;
        this.dY = dY - 32.0f;
        this.x = dX;
        this.y = dY;
        this.duration = this.startingDuration = 1.0f;
        this.scale = 0.01f;
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
        this.renderBehind = false;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.x = Interpolation.pow5In.apply(this.dX, this.tX, this.duration);
        this.y = Interpolation.pow5In.apply(this.dY, this.tY, this.duration);
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.fade.apply(1.0f, 0.01f, this.duration - this.startingDuration / 2.0f) * Settings.scale : Interpolation.fade.apply(0.01f, 1.0f, this.duration / (this.startingDuration / 2.0f)) * Settings.scale;
        this.scale = Interpolation.bounceIn.apply(5.0f, 1.0f, this.duration) * Settings.scale;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.color.a));
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.WEB_VFX, this.x, this.y, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, 0.0f, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

