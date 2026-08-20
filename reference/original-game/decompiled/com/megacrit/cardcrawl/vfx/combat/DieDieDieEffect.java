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
import com.megacrit.cardcrawl.vfx.combat.ThrowShivEffect;

public class DieDieDieEffect
extends AbstractGameEffect {
    private float interval = 0.0f;

    public DieDieDieEffect() {
        this.duration = 0.5f;
    }

    @Override
    public void update() {
        this.interval -= Gdx.graphics.getDeltaTime();
        if (this.interval < 0.0f) {
            this.interval = MathUtils.random(0.02f, 0.05f);
            int derp = MathUtils.random(1, 4);
            for (int i = 0; i < derp; ++i) {
                AbstractDungeon.effectsQueue.add(new ThrowShivEffect(MathUtils.random(1200.0f, 2000.0f) * Settings.scale, AbstractDungeon.floorY + MathUtils.random(-100.0f, 500.0f) * Settings.scale));
            }
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

