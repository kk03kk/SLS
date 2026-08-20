/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.StarBounceEffect;

public class WallopEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private int damage = 0;

    public WallopEffect(int damage, float x, float y) {
        this.damage = damage;
        this.x = x;
        this.y = y;
        if (this.damage > 50) {
            this.damage = 50;
        }
    }

    @Override
    public void update() {
        for (int i = 0; i < this.damage; ++i) {
            AbstractDungeon.effectsQueue.add(new StarBounceEffect(this.x, this.y));
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

