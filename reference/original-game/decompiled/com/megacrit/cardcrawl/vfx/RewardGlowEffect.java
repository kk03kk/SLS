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

public class RewardGlowEffect
extends AbstractGameEffect {
    private static final int W = 64;
    private static final float DURATION = 1.1f;
    private float scale;
    private float x;
    private float y;
    private float angle;

    public RewardGlowEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.color = Color.WHITE.cpy();
        this.duration = 1.1f;
        this.scale = Settings.scale;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.scale += Settings.scale * Gdx.graphics.getDeltaTime() / 20.0f;
        this.color.a = Interpolation.fade.apply(this.duration / 1.1f) / 12.0f;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.REWARD_SCREEN_ITEM, this.x - 232.0f, this.y - 49.0f, 232.0f, 49.0f, 464.0f, 98.0f, Settings.xScale, this.scale + Settings.scale * 0.05f, 0.0f, 0, 0, 464, 98, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }

    public void render(SpriteBatch sb, Color tint) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale * Settings.scale / 2.0f, this.scale * Settings.scale / 2.0f, this.angle, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }
}

