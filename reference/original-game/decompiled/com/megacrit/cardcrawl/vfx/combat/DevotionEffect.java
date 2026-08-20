/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect;

public class DevotionEffect
extends AbstractGameEffect {
    int count = 0;

    @Override
    public void update() {
        if (this.count == 0) {
            AbstractDungeon.effectsQueue.add(new BorderLongFlashEffect(Color.SKY, true));
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            ++this.count;
            this.duration = MathUtils.random(0.1f, 0.2f);
            float x = (float)(Settings.WIDTH * this.count) / 7.0f;
            float y = MathUtils.random(AbstractDungeon.floorY - 80.0f * Settings.scale, AbstractDungeon.floorY + 50.0f * Settings.scale);
            for (int i = 0; i < 5; ++i) {
                AbstractDungeon.effectsQueue.add(new TorchParticleXLEffect(x, y, MathUtils.random(1.1f, 1.6f)));
            }
        }
        if (this.count >= 6) {
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

