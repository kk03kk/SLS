/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class BorderFlashEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img = ImageMaster.BORDER_GLOW_2;
    private static final float DUR = 1.0f;
    private boolean additive;

    public BorderFlashEffect(Color color) {
        this(color, true);
    }

    public BorderFlashEffect(Color color, boolean additive) {
        this.duration = 1.0f;
        this.color = color.cpy();
        this.color.a = 0.0f;
        this.additive = additive;
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
        if (this.additive) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);
            sb.draw(this.img, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
            sb.setBlendFunction(770, 771);
        } else {
            sb.setColor(this.color);
            sb.draw(this.img, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        }
    }

    @Override
    public void dispose() {
    }
}

