/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.FrostOrbActivateParticle;

public class FrostOrbActivateEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public FrostOrbActivateEffect(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        AbstractDungeon.effectsQueue.add(new FrostOrbActivateParticle(0, this.x, this.y));
        AbstractDungeon.effectsQueue.add(new FrostOrbActivateParticle(1, this.x, this.y));
        AbstractDungeon.effectsQueue.add(new FrostOrbActivateParticle(2, this.x, this.y));
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

