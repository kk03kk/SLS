/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class EstablishmentPowerAction
extends AbstractGameAction {
    private int discountAmount;

    public EstablishmentPowerAction(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    @Override
    public void update() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!c.selfRetain && !c.retain) continue;
            c.modifyCostForCombat(-this.discountAmount);
        }
        this.isDone = true;
    }
}

