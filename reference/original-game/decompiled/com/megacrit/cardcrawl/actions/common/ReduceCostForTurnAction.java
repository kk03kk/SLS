/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;

public class ReduceCostForTurnAction
extends AbstractGameAction {
    private AbstractCard targetCard;

    public ReduceCostForTurnAction(AbstractCard card, int amt) {
        this.targetCard = card;
        this.amount = amt;
        this.duration = this.startDuration = Settings.ACTION_DUR_FASTER;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration && this.targetCard.costForTurn > 0) {
            this.targetCard.setCostForTurn(this.targetCard.costForTurn - this.amount);
        }
        this.tickDuration();
        if (Settings.FAST_MODE) {
            this.isDone = true;
        }
    }
}

