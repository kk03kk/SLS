/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.UpgradeShineParticleEffect;

public class ClangClangClangEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public ClangClangClangEffect(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        for (int i = 0; i < 30; ++i) {
            AbstractDungeon.effectsQueue.add(new UpgradeShineParticleEffect(this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y + MathUtils.random(-10.0f, 10.0f) * Settings.scale));
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

