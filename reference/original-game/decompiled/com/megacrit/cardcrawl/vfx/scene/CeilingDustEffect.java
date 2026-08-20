/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.FallingDustEffect;
import com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect;

public class CeilingDustEffect
extends AbstractGameEffect {
    private int count = 20;
    private float x;

    public CeilingDustEffect() {
        this.setPosition();
    }

    private void setPosition() {
        this.x = MathUtils.random(0.0f, 1870.0f) * Settings.scale;
    }

    @Override
    public void update() {
        if (this.count != 0) {
            int num = MathUtils.random(0, 8);
            this.count -= num;
            for (int i = 0; i < num; ++i) {
                AbstractDungeon.effectsQueue.add(new FallingDustEffect(this.x, AbstractDungeon.floorY + 640.0f * Settings.scale));
                if (!MathUtils.randomBoolean(0.8f)) continue;
                AbstractDungeon.effectsQueue.add(new CeilingDustCloudEffect(this.x, AbstractDungeon.floorY + 640.0f * Settings.scale));
            }
            if (this.count <= 0) {
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

