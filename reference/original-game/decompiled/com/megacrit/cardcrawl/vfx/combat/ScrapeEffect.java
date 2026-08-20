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
import com.megacrit.cardcrawl.vfx.combat.AnimatedSlashEffect;

public class ScrapeEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private Color color2;

    public ScrapeEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.color = Color.MAROON;
        this.color2 = Color.SCARLET;
        this.duration = this.startingDuration = 0.1f;
    }

    @Override
    public void update() {
        if (MathUtils.randomBoolean()) {
            CardCrawlGame.sound.playA("ATTACK_DAGGER_5", MathUtils.random(0.0f, -0.3f));
        } else {
            CardCrawlGame.sound.playA("ATTACK_DAGGER_6", MathUtils.random(0.0f, -0.3f));
        }
        float oX = -50.0f * Settings.scale;
        float oY = 20.0f * Settings.scale;
        float sX = -35.0f * Settings.scale;
        float sY = 20.0f * Settings.scale;
        float dX = -150.0f;
        float dY = -400.0f;
        float angle = 155.0f;
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + sX * 1.5f + oX, this.y + sY * 1.5f + oY, dX, dY, angle, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + sX * 0.5f + oX, this.y + sY * 0.5f + oY, dX, dY, angle, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x - sX * 0.5f + oX, this.y - sY * 0.5f + oY, dX, dY, angle, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x - sX * 1.5f + oX, this.y - sY * 1.5f + oY, dX, dY, angle, this.color, this.color2));
        oX = 50.0f * Settings.scale;
        oY = 20.0f * Settings.scale;
        sX = 35.0f * Settings.scale;
        sY = 20.0f * Settings.scale;
        dX = 150.0f;
        dY = -400.0f;
        angle = -155.0f;
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + sX * 1.5f + oX, this.y + sY * 1.5f + oY, dX, dY, angle, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + sX * 0.5f + oX, this.y + sY * 0.5f + oY, dX, dY, angle, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x - sX * 0.5f + oX, this.y - sY * 0.5f + oY, dX, dY, angle, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x - sX * 1.5f + oX, this.y - sY * 1.5f + oY, dX, dY, angle, this.color, this.color2));
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

