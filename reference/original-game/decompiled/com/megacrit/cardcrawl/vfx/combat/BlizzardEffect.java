/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.FallingIceEffect;

public class BlizzardEffect
extends AbstractGameEffect {
    private int frostCount;
    private boolean flipped = false;

    public BlizzardEffect(int frostCount, boolean flipped) {
        this.frostCount = 5 + frostCount;
        this.flipped = flipped;
        if (this.frostCount > 50) {
            this.frostCount = 50;
        }
    }

    @Override
    public void update() {
        CardCrawlGame.sound.playA("ORB_FROST_CHANNEL", -0.25f - (float)this.frostCount / 200.0f);
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.MED, true);
        AbstractDungeon.effectsQueue.add(new BorderLongFlashEffect(Color.SKY));
        for (int i = 0; i < this.frostCount; ++i) {
            AbstractDungeon.effectsQueue.add(new FallingIceEffect(this.frostCount, this.flipped));
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

