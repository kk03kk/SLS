/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.IntenseZoomParticle;

public class IntenseZoomEffect
extends AbstractGameEffect {
    private boolean isBlack;
    private float x;
    private float y;
    private static final int AMT = 10;

    public IntenseZoomEffect(float x, float y, boolean isBlack) {
        this.x = x;
        this.y = y;
        this.isBlack = isBlack;
    }

    @Override
    public void update() {
        if (this.isBlack) {
            AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Color.BLACK, this.isBlack));
        } else {
            AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Settings.GOLD_COLOR, this.isBlack));
        }
        for (int i = 0; i < 10; ++i) {
            AbstractDungeon.effectsQueue.add(new IntenseZoomParticle(this.x, this.y, this.isBlack));
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

