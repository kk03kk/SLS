/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class SumDamageEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 2.5f;
    private float x;
    private float y;
    private float vY;
    private static final float OFFSET_Y = 200.0f * Settings.scale;
    private int amt;
    private float scale = 3.0f * Settings.scale;
    public AbstractCreature target;

    public SumDamageEffect(AbstractCreature target, float x, float y, int amt) {
        this.duration = 2.5f;
        this.startingDuration = 2.5f;
        this.x = x;
        this.y = y + OFFSET_Y;
        this.vY = 90.0f * Settings.scale;
        this.target = target;
        this.amt = amt;
        this.color = Settings.GOLD_COLOR.cpy();
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        if (this.vY > 0.0f) {
            this.vY -= 50.0f * Settings.scale * Gdx.graphics.getDeltaTime();
        }
        this.scale = Settings.scale * this.duration / 2.5f + 1.3f;
        if (this.duration < 1.0f) {
            this.color.a = this.duration;
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        FontHelper.damageNumberFont.getData().setScale(this.scale);
        FontHelper.renderFontCentered(sb, FontHelper.damageNumberFont, Integer.toString(this.amt), this.x, this.y, this.color);
    }

    @Override
    public void dispose() {
    }

    public void refresh(int amt) {
        this.amt += amt;
        this.duration = 2.5f;
        this.color.a = 1.0f;
    }
}

