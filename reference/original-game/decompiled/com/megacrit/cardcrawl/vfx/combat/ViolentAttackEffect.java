/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.AnimatedSlashEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class ViolentAttackEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private int count = 5;

    public ViolentAttackEffect(float x, float y, Color setColor) {
        this.x = x;
        this.y = y;
        this.duration = 0.0f;
        this.color = setColor;
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            CardCrawlGame.sound.playA("ATTACK_HEAVY", MathUtils.random(0.2f, 0.5f));
            AbstractDungeon.effectsQueue.add(new AnimatedSlashEffect(this.x + MathUtils.random(-100.0f, 100.0f) * Settings.scale, this.y + MathUtils.random(-100.0f, 100.0f) * Settings.scale, 0.0f, 0.0f, MathUtils.random(360.0f), MathUtils.random(2.5f, 4.0f), this.color, this.color));
            if (MathUtils.randomBoolean()) {
                AbstractDungeon.effectsQueue.add(new FlashAtkImgEffect(this.x + MathUtils.random(-150.0f, 150.0f) * Settings.scale, this.y + MathUtils.random(-150.0f, 150.0f) * Settings.scale, AbstractGameAction.AttackEffect.BLUNT_HEAVY));
            } else {
                AbstractDungeon.effectsQueue.add(new FlashAtkImgEffect(this.x + MathUtils.random(-150.0f, 150.0f) * Settings.scale, this.y + MathUtils.random(-150.0f, 150.0f) * Settings.scale, AbstractGameAction.AttackEffect.BLUNT_LIGHT));
            }
            this.duration = MathUtils.random(0.05f, 0.1f);
            --this.count;
        }
        if (this.count == 0) {
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

