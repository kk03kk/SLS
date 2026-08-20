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

public class PingHpEffect
extends AbstractGameEffect {
    private float x;

    public PingHpEffect(float x) {
        this.duration = 1.5f;
        this.color = new Color(1.0f, 1.0f, 0.2f, 0.0f);
        this.x = x;
    }

    @Override
    public void update() {
        this.scale = Interpolation.pow5In.apply(1.15f, 1.0f, this.duration / 1.5f);
        this.color.a = Interpolation.pow2Out.apply(0.0f, 0.7f, this.duration / 1.5f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.TP_HP, this.x - 32.0f + 32.0f * Settings.scale, (float)Settings.HEIGHT - 32.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale * Settings.scale, this.scale * Settings.scale, this.rotation, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

