/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class HealPanelEffect
extends AbstractGameEffect {
    private float x;
    private static Texture img = null;

    public HealPanelEffect(float x) {
        this.x = x;
        if (img == null) {
            img = ImageMaster.loadImage("images/ui/topPanel/panel_heart_white.png");
        }
        this.color = Color.CHARTREUSE.cpy();
        this.color.a = 0.0f;
        this.duration = 1.5f;
        this.scale = Settings.scale;
    }

    @Override
    public void update() {
        this.scale = Interpolation.exp10In.apply(1.2f, 2.0f, this.duration / 1.5f) * Settings.scale;
        this.color.a = this.duration > 1.0f ? Interpolation.pow5In.apply(0.6f, 0.0f, (this.duration - 1.0f) * 2.0f) : Interpolation.fade.apply(0.0f, 0.6f, this.duration);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(img, this.x - 32.0f + 32.0f * Settings.scale, (float)Settings.HEIGHT - 32.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

