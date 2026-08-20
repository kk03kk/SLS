/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class TextCenteredEffect
extends AbstractGameEffect {
    private static final float TEXT_DURATION = 1.8f;
    private static final float DRAW_X = (float)Settings.WIDTH / 2.0f;
    private static final float DRAW_Y = (float)Settings.HEIGHT * 0.6f;
    private static final float STARTING_OFFSET_Y = 120.0f * Settings.scale;
    private static final float TARGET_OFFSET_Y = 160.0f * Settings.scale;
    private static final float LERP_RATE = 5.0f;
    private float offsetY;
    private String msg;

    public TextCenteredEffect(String msg) {
        this.duration = 1.8f;
        this.startingDuration = 1.8f;
        this.msg = msg;
        this.color = Color.WHITE.cpy();
        this.offsetY = STARTING_OFFSET_Y;
    }

    @Override
    public void update() {
        super.update();
        this.offsetY = MathUtils.lerp(this.offsetY, TARGET_OFFSET_Y, Gdx.graphics.getDeltaTime() * 5.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.msg, DRAW_X, DRAW_Y + this.offsetY, this.color);
        }
    }

    @Override
    public void dispose() {
    }
}

