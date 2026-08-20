/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.EmptyStanceParticleEffect;

public class EmptyStanceEffect
extends AbstractGameEffect {
    private int numParticles = 10;
    private float x;
    private float y;

    public EmptyStanceEffect(float x, float y) {
        this.duration = 0.0f;
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            if (this.numParticles == 10) {
                AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Color.SKY, true));
            }
            AbstractDungeon.effectsQueue.add(new EmptyStanceParticleEffect(this.x, this.y));
            AbstractDungeon.effectsQueue.add(new EmptyStanceParticleEffect(this.x, this.y));
            AbstractDungeon.effectsQueue.add(new EmptyStanceParticleEffect(this.x, this.y));
            --this.numParticles;
            if (this.numParticles <= 0) {
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

