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

public class ClawEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private Color color2;

    public ClawEffect(float x, float y, Color color1, Color color2) {
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
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + 35.0f, this.y + 35.0f, 150.0f, -150.0f, -135.0f, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x, this.y, 150.0f, -150.0f, -135.0f, this.color, this.color2));
        AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x - 35.0f, this.y - 35.0f, 150.0f, -150.0f, -135.0f, this.color, this.color2));
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

