/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.PlasmaOrbActivateParticle;

public class PlasmaOrbActivateEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public PlasmaOrbActivateEffect(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        for (int i = 0; i < 12; ++i) {
            AbstractDungeon.effectsQueue.add(new PlasmaOrbActivateParticle(this.x, this.y));
        }
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

