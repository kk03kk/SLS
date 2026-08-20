/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect;

public class SmokeBombEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public SmokeBombEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.duration = 0.2f;
    }

    @Override
    public void update() {
        if (this.duration == 0.2f) {
            CardCrawlGame.sound.play("ATTACK_WHIFF_2");
            for (int i = 0; i < 90; ++i) {
                AbstractDungeon.effectsQueue.add(new SmokeBlurEffect(this.x, this.y));
            }
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            CardCrawlGame.sound.play("APPEAR");
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

