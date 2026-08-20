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

public class BorderLongFlashEffect
extends AbstractGameEffect {
    private static final float DUR = 2.0f;
    private float startAlpha;
    private boolean additive = true;

    public BorderLongFlashEffect(Color color) {
        this(color, true);
    }

    public BorderLongFlashEffect(Color color, boolean additive) {
        this.duration = 2.0f;
        this.startAlpha = color.a;
        this.color = color.cpy();
        this.color.a = 0.0f;
        this.additive = additive;
    }

    @Override
    public void update() {
        this.color.a = 2.0f - this.duration < 0.2f ? Interpolation.fade.apply(0.0f, this.startAlpha, (2.0f - this.duration) * 10.0f) : Interpolation.pow2Out.apply(0.0f, this.startAlpha, this.duration / 2.0f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.additive) {
            sb.setBlendFunction(770, 1);
        }
        sb.setColor(this.color);
        sb.draw(ImageMaster.BORDER_GLOW_2, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        if (this.additive) {
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }
}

