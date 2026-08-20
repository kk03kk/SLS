/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class HealNumberEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 1.2f;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private static final float OFFSET_Y = 150.0f * Settings.scale;
    private static final float GRAVITY_Y = -2000.0f * Settings.scale;
    private int number;
    private float scale = 1.0f;

    public HealNumberEffect(float x, float y, int number) {
        this.duration = 1.2f;
        this.startingDuration = 1.2f;
        this.x = x;
        this.y = y + OFFSET_Y;
        this.vX = MathUtils.random(100.0f * Settings.scale, 150.0f * Settings.scale);
        if (MathUtils.randomBoolean()) {
            this.vX = -this.vX;
        }
        this.vY = MathUtils.random(400.0f * Settings.scale, 500.0f * Settings.scale);
        this.number = number;
        this.color = Color.CHARTREUSE.cpy();
    }

    @Override
    public void update() {
        this.x += Gdx.graphics.getDeltaTime() * this.vX;
        this.y += Gdx.graphics.getDeltaTime() * this.vY;
        this.vY += Gdx.graphics.getDeltaTime() * GRAVITY_Y;
        super.update();
        this.scale = Settings.scale * this.duration / 1.2f * 3.0f;
        if (this.scale <= 0.0f) {
            this.scale = 0.01f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        FontHelper.damageNumberFont.getData().setScale(this.scale);
        FontHelper.renderFontCentered(sb, FontHelper.damageNumberFont, Integer.toString(this.number), this.x, this.y, this.color);
    }

    @Override
    public void dispose() {
    }
}

