/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class RandomCardFromDiscardPileToHandAction
extends AbstractGameAction {
    private AbstractPlayer p = AbstractDungeon.player;

    public RandomCardFromDiscardPileToHandAction() {
        this.setValues(this.p, AbstractDungeon.player, this.amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
    }

    @Override
    public void update() {
        if (this.p.discardPile.size() > 0) {
            AbstractCard card = this.p.discardPile.getRandomCard(AbstractDungeon.cardRandomRng);
            this.p.hand.addToHand(card);
            card.lighten(false);
            this.p.discardPile.removeCard(card);
            this.p.hand.refreshHandLayout();
        }
        this.tickDuration();
        this.isDone = true;
    }
}

