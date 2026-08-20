/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.RedFireballEffect;

public class SearingBlowEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private int timesUpgraded;

    public SearingBlowEffect(float x, float y, int timesUpgraded) {
        this.x = x;
        this.y = y;
        this.timesUpgraded = timesUpgraded;
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, true);
    }

    @Override
    public void update() {
        CardCrawlGame.sound.playA("ATTACK_FIRE", 0.3f);
        CardCrawlGame.sound.playA("ATTACK_HEAVY", -0.3f);
        float dst = 180.0f + (float)this.timesUpgraded * 3.0f;
        AbstractDungeon.effectsQueue.add(new RedFireballEffect(this.x - dst * Settings.scale, this.y, this.x + dst * Settings.scale, this.y - 50.0f * Settings.scale, this.timesUpgraded));
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

