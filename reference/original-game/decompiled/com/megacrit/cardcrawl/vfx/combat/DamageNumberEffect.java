/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.SumDamageEffect;

public class DamageNumberEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 1.2f;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private static final float OFFSET_Y = 150.0f * Settings.scale;
    private static final float GRAVITY_Y = -2000.0f * Settings.scale;
    private int amt;
    private float scale = 1.0f;
    public AbstractCreature target;

    public DamageNumberEffect(AbstractCreature target, float x, float y, int amt) {
        this.duration = 1.2f;
        this.startingDuration = 1.2f;
        this.x = x;
        this.y = y + OFFSET_Y;
        this.target = target;
        this.vX = MathUtils.random(100.0f * Settings.scale, 150.0f * Settings.scale);
        if (MathUtils.randomBoolean()) {
            this.vX = -this.vX;
        }
        this.vY = MathUtils.random(400.0f * Settings.scale, 500.0f * Settings.scale);
        this.amt = amt;
        this.color = Color.RED.cpy();
        if (!Settings.SHOW_DMG_SUM || amt <= 0) {
            return;
        }
        boolean isSumDamageAvailable = false;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof SumDamageEffect) || ((SumDamageEffect)e).target != target) continue;
            isSumDamageAvailable = true;
            ((SumDamageEffect)e).refresh(amt);
        }
        if (!isSumDamageAvailable) {
            for (AbstractGameEffect e : AbstractDungeon.effectList) {
                if (!(e instanceof DamageNumberEffect) || e == this || ((DamageNumberEffect)e).target != target) continue;
                AbstractDungeon.effectsQueue.add(new SumDamageEffect(target, x, y, ((DamageNumberEffect)e).amt + amt));
            }
        }
    }

    @Override
    public void update() {
        this.x += Gdx.graphics.getDeltaTime() * this.vX;
        this.y += Gdx.graphics.getDeltaTime() * this.vY;
        this.vY += Gdx.graphics.getDeltaTime() * GRAVITY_Y;
        super.update();
        if (this.color.g != 1.0f) {
            this.color.g += Gdx.graphics.getDeltaTime() * 4.0f;
            if (this.color.g > 1.0f) {
                this.color.g = 1.0f;
            }
        }
        if (this.color.b != 1.0f) {
            this.color.b += Gdx.graphics.getDeltaTime() * 4.0f;
            if (this.color.b > 1.0f) {
                this.color.b = 1.0f;
            }
        }
        this.scale = Interpolation.pow4Out.apply(6.0f, 1.2f, 1.0f - this.duration / 1.2f) * Settings.scale;
    }

    @Override
    public void render(SpriteBatch sb) {
        FontHelper.damageNumberFont.getData().setScale(this.scale);
        FontHelper.renderFontCentered(sb, FontHelper.damageNumberFont, Integer.toString(this.amt), this.x, this.y, this.color);
    }

    @Override
    public void dispose() {
    }
}

