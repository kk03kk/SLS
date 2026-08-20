/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class RetreatingHandAction
extends AbstractGameAction {
    private AbstractCard card;

    public RetreatingHandAction(AbstractCard card) {
        this.card = card;
    }

    @Override
    public void update() {
        this.card.returnToHand = AbstractDungeon.actionManager.cardsPlayedThisCombat.size() >= 2 && AbstractDungeon.actionManager.cardsPlayedThisCombat.get((int)(AbstractDungeon.actionManager.cardsPlayedThisCombat.size() - 2)).type == AbstractCard.CardType.ATTACK;
        this.isDone = true;
    }
}

