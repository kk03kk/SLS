/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect;

public class RedFireballEffect
extends AbstractGameEffect {
    private static final float FIREBALL_INTERVAL = 0.016f;
    private float x;
    private float y;
    private float startX;
    private float startY;
    private float targetX;
    private float targetY;
    private float vfxTimer = 0.0f;
    private int timesUpgraded;

    public RedFireballEffect(float startX, float startY, float targetX, float targetY, int timesUpgraded) {
        this.startingDuration = 0.3f;
        this.duration = 0.3f;
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.timesUpgraded = timesUpgraded;
        this.x = startX;
        this.y = startY;
    }

    @Override
    public void update() {
        this.x = Interpolation.fade.apply(this.targetX, this.startX, this.duration / this.startingDuration);
        this.y = this.duration > this.startingDuration / 2.0f ? Interpolation.pow4In.apply(this.startY, this.targetY, (this.duration - this.startingDuration / 2.0f) / this.startingDuration * 2.0f) : Interpolation.pow4Out.apply(this.targetY, this.startY, this.duration / this.startingDuration * 2.0f);
        this.vfxTimer -= Gdx.graphics.getDeltaTime();
        if (this.vfxTimer < 0.0f) {
            this.vfxTimer += 0.016f;
            AbstractDungeon.effectsQueue.add(new RedFireBurstParticleEffect(this.x, this.y, this.timesUpgraded));
            AbstractDungeon.effectsQueue.add(new RedFireBurstParticleEffect(this.x, this.y, this.timesUpgraded));
        }
        this.duration -= Gdx.graphics.getDeltaTime();
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

