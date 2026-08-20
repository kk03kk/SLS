/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.BloodShotParticleEffect;

public class BloodShotEffect
extends AbstractGameEffect {
    private float sX;
    private float sY;
    private float tX;
    private float tY;
    private int count = 0;
    private float timer = 0.0f;

    public BloodShotEffect(float sX, float sY, float tX, float tY, int count) {
        this.sX = sX - 20.0f * Settings.scale;
        this.sY = sY + 80.0f * Settings.scale;
        this.tX = tX;
        this.tY = tY;
        this.count = count;
    }

    @Override
    public void update() {
        this.timer -= Gdx.graphics.getDeltaTime();
        if (this.timer < 0.0f) {
            this.timer += MathUtils.random(0.05f, 0.15f);
            AbstractDungeon.effectsQueue.add(new BloodShotParticleEffect(this.sX, this.sY, this.tX, this.tY));
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

