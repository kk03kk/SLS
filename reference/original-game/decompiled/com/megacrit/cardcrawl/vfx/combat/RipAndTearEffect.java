/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.AnimatedSlashEffect;

public class RipAndTearEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private Color color2;
    private static boolean flipped = false;

    public RipAndTearEffect(float x, float y, Color color1, Color color2) {
        this.x = x;
        this.y = y;
        this.color = color1;
        this.color2 = color2;
        this.duration = this.startingDuration = 0.1f;
    }

    @Override
    public void update() {
        if (MathUtils.randomBoolean()) {
            CardCrawlGame.sound.playA("ATTACK_DAGGER_5", MathUtils.random(0.0f, -0.3f));
        } else {
            CardCrawlGame.sound.playA("ATTACK_DAGGER_6", MathUtils.random(0.0f, -0.3f));
        }
        if (!flipped) {
            float baseAngle = 135.0f;
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x - 45.0f, this.y + 45.0f, -150.0f, -150.0f, baseAngle + MathUtils.random(-10.0f, 10.0f), this.color, this.color2));
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x, this.y, -150.0f, -150.0f, baseAngle + MathUtils.random(-10.0f, 10.0f), this.color, this.color2));
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + 45.0f, this.y - 45.0f, -150.0f, -150.0f, baseAngle + MathUtils.random(-10.0f, 10.0f), this.color, this.color2));
        } else {
            float baseAngle = -135.0f;
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x - 45.0f, this.y - 45.0f, 150.0f, -150.0f, baseAngle + MathUtils.random(-10.0f, 10.0f), this.color, this.color2));
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x, this.y, 150.0f, -150.0f, baseAngle + MathUtils.random(-10.0f, 10.0f), this.color, this.color2));
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + 40.0f, this.y + 40.0f, 150.0f, -150.0f, baseAngle + MathUtils.random(-10.0f, 10.0f), this.color, this.color2));
        }
        flipped = !flipped;
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

