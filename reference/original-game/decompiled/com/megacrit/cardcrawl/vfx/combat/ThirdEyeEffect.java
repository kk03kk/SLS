/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.ThirdEyeParticleEffect;

public class ThirdEyeEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public ThirdEyeEffect(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, 800.0f, 0.0f));
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, -800.0f, 0.0f));
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, 0.0f, 500.0f));
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, 0.0f, -500.0f));
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, 600.0f, 0.0f));
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, -600.0f, 0.0f));
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, 0.0f, 400.0f));
        AbstractDungeon.effectsQueue.add(new ThirdEyeParticleEffect(this.x, this.y, 0.0f, -400.0f));
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

