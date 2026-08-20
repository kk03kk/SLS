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

public class RoomTintEffect
extends AbstractGameEffect {
    private static final float DEFAULT_DUR = 2.0f;
    private float tintTransparency;

    public RoomTintEffect(Color color, float tintTransparency) {
        this(color, tintTransparency, 2.0f, true);
    }

    public RoomTintEffect(Color color, float tintTransparency, float setDuration, boolean renderBehind) {
        this.renderBehind = renderBehind;
        this.startingDuration = setDuration;
        this.duration = setDuration;
        this.color = color;
        this.color.a = 0.0f;
        this.tintTransparency = tintTransparency;
    }

    @Override
    public void update() {
        if (this.duration > this.startingDuration * 0.5f) {
            this.color.a = Interpolation.fade.apply(this.tintTransparency, 0.0f, (this.duration - this.startingDuration * 0.5f) / this.startingDuration);
        } else if (this.duration < this.startingDuration * 0.5f) {
            this.color.a = Interpolation.fade.apply(0.0f, this.tintTransparency, this.duration / this.startingDuration / 0.5f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    @Override
    public void dispose() {
    }
}

