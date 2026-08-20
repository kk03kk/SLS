/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class InversionBeamEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public InversionBeamEffect(float x) {
        this.duration = this.startingDuration = 0.5f;
        this.x = x;
        this.y = 0.01f;
        this.renderBehind = MathUtils.randomBoolean();
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else {
            this.y = this.duration < this.startingDuration / 2.0f ? Interpolation.fade.apply(0.01f, 50.0f, this.duration / (this.startingDuration / 2.0f)) * Settings.scale : Interpolation.fade.apply(50.0f, 0.01f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f)) * Settings.scale;
        }
        this.scale = Interpolation.bounce.apply(0.01f, 5.0f, this.duration / this.startingDuration);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(775, 769);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.x - this.y / 2.0f, 0.0f, this.y, (float)Settings.HEIGHT - 64.0f * Settings.scale);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

