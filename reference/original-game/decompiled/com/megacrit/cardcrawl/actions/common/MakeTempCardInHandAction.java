/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;

public class MakeTempCardInHandAction
extends AbstractGameAction {
    private AbstractCard c;
    private static final float PADDING = 25.0f * Settings.scale;
    private boolean isOtherCardInCenter = true;
    private boolean sameUUID = false;

    public MakeTempCardInHandAction(AbstractCard card, boolean isOtherCardInCenter) {
        UnlockTracker.markCardAsSeen(card.cardID);
        this.amount = 1;
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.c = card;
        if (this.c.type != AbstractCard.CardType.CURSE && this.c.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
            this.c.upgrade();
        }
        this.isOtherCardInCenter = isOtherCardInCenter;
    }

    public MakeTempCardInHandAction(AbstractCard card) {
        this(card, 1);
    }

    public MakeTempCardInHandAction(AbstractCard card, int amount) {
        UnlockTracker.markCardAsSeen(card.cardID);
        this.amount = amount;
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.c = card;
        if (this.c.type != AbstractCard.CardType.CURSE && this.c.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
            this.c.upgrade();
        }
    }

    public MakeTempCardInHandAction(AbstractCard card, int amount, boolean isOtherCardInCenter) {
        this(card, amount);
        this.isOtherCardInCenter = isOtherCardInCenter;
    }

    public MakeTempCardInHandAction(AbstractCard card, boolean isOtherCardInCenter, boolean sameUUID) {
        this(card, 1);
        this.isOtherCardInCenter = isOtherCardInCenter;
        this.sameUUID = sameUUID;
    }

    @Override
    public void update() {
        if (this.amount == 0) {
            this.isDone = true;
            return;
        }
        int discardAmount = 0;
        int handAmount = this.amount;
        if (this.amount + AbstractDungeon.player.hand.size() > 10) {
            AbstractDungeon.player.createHandIsFullDialog();
            discardAmount = this.amount + AbstractDungeon.player.hand.size() - 10;
            handAmount -= discardAmount;
        }
        this.addToHand(handAmount);
        this.addToDiscard(discardAmount);
        if (this.amount > 0) {
            this.addToTop(new WaitAction(0.8f));
        }
        this.isDone = true;
    }

    private void addToHand(int handAmt) {
        switch (this.amount) {
            case 0: {
                break;
            }
            case 1: {
                if (handAmt != 1) break;
                if (this.isOtherCardInCenter) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f - (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT / 2.0f));
                    break;
                }
                AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard()));
                break;
            }
            case 2: {
                if (handAmt == 1) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f - (PADDING + AbstractCard.IMG_WIDTH * 0.5f), (float)Settings.HEIGHT / 2.0f));
                    break;
                }
                if (handAmt != 2) break;
                AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f + (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f - (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT / 2.0f));
                break;
            }
            case 3: {
                if (handAmt == 1) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f - (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT / 2.0f));
                    break;
                }
                if (handAmt == 2) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f + (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT / 2.0f));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f - (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT / 2.0f));
                    break;
                }
                if (handAmt != 3) break;
                for (int i = 0; i < this.amount; ++i) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard()));
                }
                break;
            }
            default: {
                for (int i = 0; i < handAmt; ++i) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(this.makeNewCard(), MathUtils.random((float)Settings.WIDTH * 0.2f, (float)Settings.WIDTH * 0.8f), MathUtils.random((float)Settings.HEIGHT * 0.3f, (float)Settings.HEIGHT * 0.7f)));
                }
            }
        }
    }

    private void addToDiscard(int discardAmt) {
        switch (this.amount) {
            case 0: {
                break;
            }
            case 1: {
                if (discardAmt != 1) break;
                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH / 2.0f + (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT / 2.0f));
                break;
            }
            case 2: {
                if (discardAmt == 1) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f - (PADDING + AbstractCard.IMG_WIDTH * 0.5f), (float)Settings.HEIGHT * 0.5f));
                    break;
                }
                if (discardAmt != 2) break;
                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f - (PADDING + AbstractCard.IMG_WIDTH * 0.5f), (float)Settings.HEIGHT * 0.5f));
                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f + (PADDING + AbstractCard.IMG_WIDTH * 0.5f), (float)Settings.HEIGHT * 0.5f));
                break;
            }
            case 3: {
                if (discardAmt == 1) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f + (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT * 0.5f));
                    break;
                }
                if (discardAmt == 2) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f, (float)Settings.HEIGHT * 0.5f));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f + (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT * 0.5f));
                    break;
                }
                if (discardAmt != 3) break;
                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f, (float)Settings.HEIGHT * 0.5f));
                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f - (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT * 0.5f));
                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), (float)Settings.WIDTH * 0.5f + (PADDING + AbstractCard.IMG_WIDTH), (float)Settings.HEIGHT * 0.5f));
                break;
            }
            default: {
                for (int i = 0; i < discardAmt; ++i) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(this.makeNewCard(), MathUtils.random((float)Settings.WIDTH * 0.2f, (float)Settings.WIDTH * 0.8f), MathUtils.random((float)Settings.HEIGHT * 0.3f, (float)Settings.HEIGHT * 0.7f)));
                }
            }
        }
    }

    private AbstractCard makeNewCard() {
        if (this.sameUUID) {
            return this.c.makeSameInstanceOf();
        }
        return this.c.makeStatEquivalentCopy();
    }
}

