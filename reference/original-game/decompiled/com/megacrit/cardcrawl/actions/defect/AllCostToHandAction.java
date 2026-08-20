/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.DiscardToHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class AllCostToHandAction
extends AbstractGameAction {
    private AbstractPlayer p = AbstractDungeon.player;
    private int costTarget;

    public AllCostToHandAction(int costToTarget) {
        this.setValues(this.p, AbstractDungeon.player, this.amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.costTarget = costToTarget;
    }

    @Override
    public void update() {
        if (this.p.discardPile.size() > 0) {
            for (AbstractCard card : this.p.discardPile.group) {
                if (card.cost != this.costTarget && !card.freeToPlayOnce) continue;
                this.addToBot(new DiscardToHandAction(card));
            }
        }
        this.tickDuration();
        this.isDone = true;
    }
}

