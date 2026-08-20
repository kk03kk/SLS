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
import com.megacrit.cardcrawl.vfx.FastSmokeParticle;

public class SmokingEmberEffect
extends AbstractGameEffect {
    private float x;
    private float vX;
    private float y;
    private float vY;
    private float gravity;
    private static final float INTERVAL = 0.01f;
    private float smokeTimer = 0.0f;

    public SmokingEmberEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.vX = MathUtils.random(-600.0f, 600.0f) * Settings.scale;
        this.vY = MathUtils.random(-200.0f, 600.0f) * Settings.scale;
        this.gravity = 800.0f * Settings.scale;
        this.scale = MathUtils.random(0.2f, 0.4f) * Settings.scale;
        this.duration = MathUtils.random(0.3f, 0.6f);
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vY -= this.gravity * Gdx.graphics.getDeltaTime();
        this.smokeTimer -= Gdx.graphics.getDeltaTime();
        if (this.smokeTimer < 0.0f) {
            this.smokeTimer = 0.01f;
            AbstractDungeon.effectsQueue.add(new FastSmokeParticle(this.x, this.y));
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

