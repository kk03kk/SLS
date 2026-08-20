/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.CollectorStakeEffect;
import com.megacrit.cardcrawl.vfx.combat.RoomTintEffect;

public class CollectorCurseEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private int count;
    private float stakeTimer = 0.0f;

    public CollectorCurseEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.count = 13;
    }

    @Override
    public void update() {
        this.stakeTimer -= Gdx.graphics.getDeltaTime();
        if (this.stakeTimer < 0.0f) {
            if (this.count == 13) {
                CardCrawlGame.sound.playA("ATTACK_HEAVY", -0.5f);
                AbstractDungeon.effectsQueue.add(new RoomTintEffect(Color.BLACK.cpy(), 0.8f));
                AbstractDungeon.effectsQueue.add(new BorderLongFlashEffect(new Color(1.0f, 0.0f, 1.0f, 0.7f)));
            }
            AbstractDungeon.effectsQueue.add(new CollectorStakeEffect(this.x + MathUtils.random(-50.0f, 50.0f) * Settings.scale, this.y + MathUtils.random(-60.0f, 60.0f) * Settings.scale));
            this.stakeTimer = 0.04f;
            --this.count;
            if (this.count == 0) {
                this.isDone = true;
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

