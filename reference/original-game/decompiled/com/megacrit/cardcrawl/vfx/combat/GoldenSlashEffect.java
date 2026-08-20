/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.AnimatedSlashEffect;

public class GoldenSlashEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private boolean isVertical;

    public GoldenSlashEffect(float x, float y, boolean isVertical) {
        this.x = x;
        this.y = y;
        this.duration = this.startingDuration = 0.1f;
        this.isVertical = isVertical;
    }

    @Override
    public void update() {
        CardCrawlGame.sound.playA("ATTACK_IRON_2", -0.4f);
        CardCrawlGame.sound.playA("ATTACK_HEAVY", -0.4f);
        if (this.isVertical) {
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x, this.y - 30.0f * Settings.scale, 0.0f, -500.0f, 180.0f, 5.0f, Color.GOLD, Color.GOLD));
        } else {
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x, this.y - 30.0f * Settings.scale, -500.0f, -500.0f, 135.0f, 4.0f, Color.GOLD, Color.GOLD));
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

