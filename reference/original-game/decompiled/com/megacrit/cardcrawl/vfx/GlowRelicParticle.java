/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class GlowRelicParticle
extends AbstractGameEffect {
    private static final float DURATION = 3.0f;
    private float scale = 0.01f;
    private static final int IMG_W = 128;
    private Texture img;
    private float x;
    private float y;

    public GlowRelicParticle(Texture img, float x, float y, float angle) {
        this.duration = 3.0f;
        this.img = img;
        this.x = x;
        this.y = y;
        this.rotation = angle;
        this.color = Color.WHITE.cpy();
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale = Interpolation.fade.apply(Settings.scale, 2.0f * Settings.scale, 1.0f - this.duration / 3.0f);
        this.color.a = Interpolation.fade.apply(1.0f, 0.0f, 1.0f - this.duration / 3.0f) / 2.0f;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x - 64.0f, this.y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

