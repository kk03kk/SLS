/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class SilentGainPowerEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 2.0f;
    private float scale;
    private TextureAtlas.AtlasRegion region48 = null;

    public SilentGainPowerEffect(AbstractPower power) {
        if (power.img == null) {
            this.region48 = power.region48;
        }
        this.duration = 2.0f;
        this.startingDuration = 2.0f;
        this.scale = Settings.scale;
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.5f);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration > 0.5f) {
            this.scale = Interpolation.exp5Out.apply(3.0f * Settings.scale, Settings.scale, -(this.duration - 2.0f) / 1.5f);
        } else {
            this.color.a = Interpolation.fade.apply(0.5f, 0.0f, 1.0f - this.duration);
        }
    }

    @Override
    public void render(SpriteBatch sb, float x, float y) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        if (this.region48 != null) {
            sb.draw(this.region48, x - (float)this.region48.packedWidth / 2.0f, y - (float)this.region48.packedHeight / 2.0f, (float)this.region48.packedWidth / 2.0f, (float)this.region48.packedHeight / 2.0f, this.region48.packedWidth, this.region48.packedHeight, this.scale, this.scale, 0.0f);
        }
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
    }
}

