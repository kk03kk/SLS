/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.UpgradeShineParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.AnimatedSlashEffect;

public class ClashEffect
extends AbstractGameEffect {
    private float x;
    private float y;

    public ClashEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.duration = this.startingDuration = 0.1f;
    }

    @Override
    public void update() {
        CardCrawlGame.sound.playA("ATTACK_WHIFF_1", 0.4f);
        CardCrawlGame.sound.playA("ATTACK_IRON_1", -0.1f);
        CardCrawlGame.sound.playA("ATTACK_IRON_3", -0.1f);
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x, this.y - 30.0f * Settings.scale, -500.0f, -500.0f, 135.0f, 4.0f, Color.SCARLET, Color.GOLD));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x, this.y - 30.0f * Settings.scale, 500.0f, -500.0f, 225.0f, 4.0f, Color.SKY, Color.CYAN));
        for (int i = 0; i < 15; ++i) {
            AbstractDungeon.effectsQueue.add(new UpgradeShineParticleEffect(this.x + MathUtils.random(-40.0f, 40.0f) * Settings.scale, this.y + MathUtils.random(-40.0f, 40.0f) * Settings.scale));
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

