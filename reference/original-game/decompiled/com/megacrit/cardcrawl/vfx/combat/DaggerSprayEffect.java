/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.FlyingDaggerEffect;

public class DaggerSprayEffect
extends AbstractGameEffect {
    private boolean flipX;

    public DaggerSprayEffect(boolean shouldFlip) {
        this.flipX = shouldFlip;
    }

    @Override
    public void update() {
        this.isDone = true;
        if (this.flipX) {
            for (int i = 12; i > 0; --i) {
                float x = AbstractDungeon.player.hb.cX - MathUtils.random(0.0f, 450.0f) * Settings.scale;
                AbstractDungeon.effectsQueue.add(new FlyingDaggerEffect(x, AbstractDungeon.player.hb.cY + 120.0f * Settings.scale + (float)i * -18.0f * Settings.scale, (float)(i * 4) - 30.0f, true));
            }
        } else {
            for (int i = 0; i < 12; ++i) {
                float x = AbstractDungeon.player.hb.cX + MathUtils.random(0.0f, 450.0f) * Settings.scale;
                AbstractDungeon.effectsQueue.add(new FlyingDaggerEffect(x, AbstractDungeon.player.hb.cY - 100.0f * Settings.scale + (float)i * 18.0f * Settings.scale, (float)(i * 4) - 20.0f, false));
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

