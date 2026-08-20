/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class RandomizeHandCostAction
extends AbstractGameAction {
    private AbstractPlayer p;

    public RandomizeHandCostAction() {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            for (AbstractCard card : this.p.hand.group) {
                int newCost;
                if (card.cost < 0 || card.cost == (newCost = AbstractDungeon.cardRandomRng.random(3))) continue;
                card.costForTurn = card.cost = newCost;
                card.isCostModified = true;
            }
            this.isDone = true;
            return;
        }
        this.tickDuration();
    }
}

