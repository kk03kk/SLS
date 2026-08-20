/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.PetalEffect;
import com.megacrit.cardcrawl.vfx.SpotlightEffect;

public class GrandFinalEffect
extends AbstractGameEffect {
    private float timer = 0.1f;

    public GrandFinalEffect() {
        this.duration = 2.0f;
    }

    @Override
    public void update() {
        if (this.duration == 2.0f) {
            AbstractDungeon.effectsQueue.add(new SpotlightEffect());
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.timer -= Gdx.graphics.getDeltaTime();
        if (this.timer < 0.0f) {
            this.timer += 0.1f;
            AbstractDungeon.effectsQueue.add(new PetalEffect());
            AbstractDungeon.effectsQueue.add(new PetalEffect());
        }
        if (this.duration < 0.0f) {
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

