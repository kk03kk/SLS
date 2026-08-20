/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class NecronomicurseEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 1.5f;
    private static final float FAST_DUR = 0.5f;
    private AbstractCard card;
    private static final float PADDING = 30.0f * Settings.scale;
    private float waitTimer = 2.0f;

    public NecronomicurseEffect(AbstractCard card, float x, float y) {
        this.card = card;
        this.duration = Settings.FAST_MODE ? 0.5f : 1.5f;
        this.identifySpawnLocation(x, y);
        card.drawScale = 0.01f;
        card.targetDrawScale = 1.0f;
        CardCrawlGame.sound.play("CARD_SELECT");
        AbstractDungeon.player.masterDeck.addToTop(card);
    }

    private void identifySpawnLocation(float x, float y) {
        int effectCount = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof NecronomicurseEffect)) continue;
            ++effectCount;
        }
        this.card.current_x = x;
        this.card.current_y = y;
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
        if (this.waitTimer > 0.0f) {
            this.waitTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.card.current_x += MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.card.current_y += MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.card.update();
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.card.shrink();
            AbstractDungeon.getCurrRoom().souls.obtain(this.card, false);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone && this.waitTimer < 0.0f) {
            this.card.render(sb);
        }
    }

    @Override
    public void dispose() {
    }
}

