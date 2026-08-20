/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.FireBurstParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.LightFlareParticleEffect;

public class GhostIgniteEffect
extends AbstractGameEffect {
    private static final int COUNT = 25;
    private float x;
    private float y;

    public GhostIgniteEffect(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        for (int i = 0; i < 25; ++i) {
            AbstractDungeon.effectsQueue.add(new FireBurstParticleEffect(this.x, this.y));
            AbstractDungeon.effectsQueue.add(new LightFlareParticleEffect(this.x, this.y, Color.CHARTREUSE));
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

