/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.VerticalAuraParticle;

public class VerticalAuraEffect
extends AbstractGameEffect {
    private static final float NUM_PARTICLES = 20.0f;
    private float x;
    private float y;

    public VerticalAuraEffect(Color c, float x, float y) {
        this.color = c;
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        int i = 0;
        while ((float)i < 20.0f) {
            AbstractDungeon.effectsQueue.add(new VerticalAuraParticle(this.color, this.x, this.y));
            ++i;
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

