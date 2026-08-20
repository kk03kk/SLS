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

public class WarningSignEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public WarningSignEffect(float x, float y) {
        this.duration = 1.0f;
        this.color = Color.SCARLET.cpy();
        this.color.a = 0.0f;
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        this.color.a = 1.0f - this.duration < 0.1f ? Interpolation.fade.apply(0.0f, 1.0f, (1.0f - this.duration) * 10.0f) : Interpolation.pow2Out.apply(0.0f, 1.0f, this.duration);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(ImageMaster.WARNING_ICON_VFX, this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

