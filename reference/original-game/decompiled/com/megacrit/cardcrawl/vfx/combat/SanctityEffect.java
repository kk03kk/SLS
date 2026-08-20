/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.LightRayFlyOutEffect;

public class SanctityEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vfxTimer;
    private int count = 10;

    public SanctityEffect(float newX, float newY) {
        this.x = newX;
        this.y = newY;
    }

    @Override
    public void update() {
        this.vfxTimer -= Gdx.graphics.getDeltaTime();
        if (this.vfxTimer < 0.0f) {
            --this.count;
            this.vfxTimer = MathUtils.random(0.0f, 0.02f);
            for (int i = 0; i < 3; ++i) {
                AbstractDungeon.effectsQueue.add(new LightRayFlyOutEffect(this.x, this.y, new Color(1.0f, 0.9f, 0.7f, 0.0f)));
            }
        }
        if (this.count <= 0) {
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

