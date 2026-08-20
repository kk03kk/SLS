/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.SwirlyBloodEffect;

public class HeartBuffEffect
extends AbstractGameEffect {
    float x;
    float y;

    public HeartBuffEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.duration = 0.5f;
        this.scale = 0.0f;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            CardCrawlGame.sound.playA("BUFF_2", -0.6f);
        }
        this.scale -= Gdx.graphics.getDeltaTime();
        if (this.scale < 0.0f) {
            this.scale = 0.05f;
            AbstractDungeon.effectsQueue.add(new SwirlyBloodEffect(this.x + MathUtils.random(-150.0f, 150.0f) * Settings.scale, this.y + MathUtils.random(-150.0f, 150.0f) * Settings.scale));
            AbstractDungeon.effectsQueue.add(new SwirlyBloodEffect(this.x + MathUtils.random(-150.0f, 150.0f) * Settings.scale, this.y + MathUtils.random(-150.0f, 150.0f) * Settings.scale));
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

