/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.DarkSmokePuffEffect;
import com.megacrit.cardcrawl.vfx.combat.SmokingEmberEffect;

public class ExplosionSmallEffect
extends AbstractGameEffect {
    private static final int EMBER_COUNT = 12;
    private float x;
    private float y;

    public ExplosionSmallEffect(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        AbstractDungeon.effectsQueue.add(new DarkSmokePuffEffect(this.x, this.y));
        for (int i = 0; i < 12; ++i) {
            AbstractDungeon.effectsQueue.add(new SmokingEmberEffect(this.x + MathUtils.random(-50.0f, 50.0f) * Settings.scale, this.y + MathUtils.random(-50.0f, 50.0f) * Settings.scale));
        }
        CardCrawlGame.sound.playA("ATTACK_FIRE", MathUtils.random(-0.2f, -0.1f));
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

