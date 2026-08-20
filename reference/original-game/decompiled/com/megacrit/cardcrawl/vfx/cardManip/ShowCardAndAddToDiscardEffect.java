/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.cardManip;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.CardPoofEffect;

public class ShowCardAndAddToDiscardEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 1.5f;
    private AbstractCard card;
    private static final float PADDING = 30.0f * Settings.scale;

    public ShowCardAndAddToDiscardEffect(AbstractCard srcCard, float x, float y) {
        this.card = srcCard.makeStatEquivalentCopy();
        this.duration = 1.5f;
        this.card.target_x = x;
        this.card.target_y = y;
        AbstractDungeon.effectsQueue.add(new CardPoofEffect(this.card.target_x, this.card.target_y));
        this.card.drawScale = 0.75f;
        this.card.targetDrawScale = 0.75f;
        CardCrawlGame.sound.play("CARD_OBTAIN");
        if (this.card.type != AbstractCard.CardType.CURSE && this.card.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
            this.card.upgrade();
        }
        AbstractDungeon.player.discardPile.addToTop(srcCard);
    }

    public ShowCardAndAddToDiscardEffect(AbstractCard card) {
        this.card = card;
        this.duration = 1.5f;
        this.identifySpawnLocation((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f);
        AbstractDungeon.effectsQueue.add(new CardPoofEffect(card.target_x, card.target_y));
        card.drawScale = 0.01f;
        card.targetDrawScale = 1.0f;
        if (card.type != AbstractCard.CardType.CURSE && card.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
            card.upgrade();
        }
        AbstractDungeon.player.discardPile.addToTop(card);
    }

    private void identifySpawnLocation(float x, float y) {
        int effectCount = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof ShowCardAndAddToDiscardEffect)) continue;
            ++effectCount;
        }
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
        this.card.update();
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.card.shrink();
            AbstractDungeon.getCurrRoom().souls.discard(this.card, true);
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

