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

public class EndTurnGlowEffect
extends AbstractGameEffect {
    private static final float DURATION = 2.0f;
    private float scale = 0.0f;
    private static final int IMG_W = 256;

    public EndTurnGlowEffect() {
        this.duration = 2.0f;
        this.color = Color.WHITE.cpy();
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale = Interpolation.fade.apply(Settings.scale, 2.0f * Settings.scale, 1.0f - this.duration / 2.0f);
        this.color.a = Interpolation.fade.apply(0.4f, 0.0f, 1.0f - this.duration / 2.0f) / 2.0f;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb, float x, float y) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(ImageMaster.END_TURN_BUTTON_GLOW, x - 128.0f, y - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, this.scale, this.scale, 0.0f, 0, 0, 256, 256, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
    }
}

