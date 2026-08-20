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

public class EndTurnLongPressBarFlashEffect
extends AbstractGameEffect {
    private Color bgColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);

    public EndTurnLongPressBarFlashEffect() {
        this.duration = 1.0f;
        this.color = new Color(1.0f, 1.0f, 0.6f, 0.0f);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = Interpolation.exp5Out.apply(0.0f, 1.0f, this.duration);
    }

    @Override
    public void render(SpriteBatch sb) {
        this.bgColor.a = this.color.a * 0.25f;
        sb.setColor(this.bgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 1533.0f * Settings.xScale, 256.0f * Settings.scale, 214.0f * Settings.scale, 20.0f * Settings.scale);
        sb.setBlendFunction(770, 1);
        this.color.r = 1.0f;
        this.color.g = 1.0f;
        this.color.b = 0.6f;
        sb.setColor(this.color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 1540.0f * Settings.xScale, 263.0f * Settings.scale, 200.0f * Settings.scale, 6.0f * Settings.scale);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

