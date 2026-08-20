/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.cardManip;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ShowCardBrieflyEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 2.5f;
    private AbstractCard card;
    private static final float PADDING = 30.0f * Settings.scale;

    public ShowCardBrieflyEffect(AbstractCard card) {
        this.card = card;
        this.duration = 2.5f;
        this.startingDuration = 2.5f;
        this.identifySpawnLocation((float)Settings.WIDTH - 96.0f * Settings.scale, (float)Settings.HEIGHT - 32.0f * Settings.scale);
        card.drawScale = 0.01f;
        card.targetDrawScale = 1.0f;
    }

    public ShowCardBrieflyEffect(AbstractCard card, float x, float y) {
        this.card = card;
        this.duration = 2.5f;
        this.startingDuration = 2.5f;
        this.card.drawScale = 0.01f;
        this.card.targetDrawScale = 1.0f;
        this.card.current_x = (float)Settings.WIDTH / 2.0f;
        this.card.current_y = (float)Settings.HEIGHT / 2.0f;
        this.card.target_x = x;
        this.card.target_y = y;
    }

    private void identifySpawnLocation(float x, float y) {
        int effectCount = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof ShowCardBrieflyEffect)) continue;
            ++effectCount;
        }
        this.card.current_x = (float)Settings.WIDTH / 2.0f;
        this.card.current_y = (float)Settings.HEIGHT / 2.0f;
        this.card.target_y = (float)Settings.HEIGHT * 0.5f;
        switch (effectCount) {
            case 0: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f;
                break;
            }
            case 1: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f - PADDING - AbstractCard.IMG_WIDTH;
                break;
            }
            case 2: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f + PADDING + AbstractCard.IMG_WIDTH;
                break;
            }
            case 3: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f - (PADDING + AbstractCard.IMG_WIDTH) * 2.0f;
                break;
            }
            case 4: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f + (PADDING + AbstractCard.IMG_WIDTH) * 2.0f;
                break;
            }
            default: {
                this.card.target_x = MathUtils.random((float)Settings.WIDTH * 0.1f, (float)Settings.WIDTH * 0.9f);
                this.card.target_y = MathUtils.random((float)Settings.HEIGHT * 0.2f, (float)Settings.HEIGHT * 0.8f);
            }
        }
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.6f) {
            this.card.fadingOut = true;
        }
        this.card.update();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            this.card.render(sb);
        }
    }

    @Override
    public void dispose() {
    }
}

