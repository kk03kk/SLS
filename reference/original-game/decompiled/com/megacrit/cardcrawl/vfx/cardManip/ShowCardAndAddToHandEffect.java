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
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.CardPoofEffect;

public class ShowCardAndAddToHandEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 0.8f;
    private AbstractCard card;
    private static final float PADDING = 25.0f * Settings.scale;

    public ShowCardAndAddToHandEffect(AbstractCard card, float offsetX, float offsetY) {
        this.card = card;
        UnlockTracker.markCardAsSeen(card.cardID);
        card.current_x = (float)Settings.WIDTH / 2.0f;
        card.current_y = (float)Settings.HEIGHT / 2.0f;
        card.target_x = offsetX;
        card.target_y = offsetY;
        this.duration = 0.8f;
        card.drawScale = 0.75f;
        card.targetDrawScale = 0.75f;
        card.transparency = 0.01f;
        card.targetTransparency = 1.0f;
        card.fadingOut = false;
        this.playCardObtainSfx();
        if (card.type != AbstractCard.CardType.CURSE && card.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
            card.upgrade();
        }
        AbstractDungeon.player.hand.addToHand(card);
        card.triggerWhenCopied();
        AbstractDungeon.player.hand.refreshHandLayout();
        AbstractDungeon.player.hand.applyPowers();
        AbstractDungeon.player.onCardDrawOrDiscard();
        if (AbstractDungeon.player.hasPower("Corruption") && card.type == AbstractCard.CardType.SKILL) {
            card.setCostForTurn(-9);
        }
    }

    public ShowCardAndAddToHandEffect(AbstractCard card) {
        this.card = card;
        this.identifySpawnLocation((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f);
        this.duration = 0.8f;
        card.drawScale = 0.75f;
        card.targetDrawScale = 0.75f;
        card.transparency = 0.01f;
        card.targetTransparency = 1.0f;
        card.fadingOut = false;
        if (card.type != AbstractCard.CardType.CURSE && card.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
            card.upgrade();
        }
        AbstractDungeon.player.hand.addToHand(card);
        card.triggerWhenCopied();
        AbstractDungeon.player.hand.refreshHandLayout();
        AbstractDungeon.player.hand.applyPowers();
        AbstractDungeon.player.onCardDrawOrDiscard();
        if (AbstractDungeon.player.hasPower("Corruption") && card.type == AbstractCard.CardType.SKILL) {
            card.setCostForTurn(-9);
        }
    }

    private void playCardObtainSfx() {
        int effectCount = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof ShowCardAndAddToHandEffect)) continue;
            ++effectCount;
        }
        if (effectCount < 2) {
            CardCrawlGame.sound.play("CARD_OBTAIN");
        }
    }

    private void identifySpawnLocation(float x, float y) {
        int effectCount = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof ShowCardAndAddToHandEffect)) continue;
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
        this.card.current_x = this.card.target_x;
        this.card.current_y = this.card.target_y - 200.0f * Settings.scale;
        AbstractDungeon.effectsQueue.add(new CardPoofEffect(this.card.target_x, this.card.target_y));
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
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

