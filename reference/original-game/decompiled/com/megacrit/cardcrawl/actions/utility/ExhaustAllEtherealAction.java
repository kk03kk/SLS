/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ExhaustAllEtherealAction
extends AbstractGameAction {
    public ExhaustAllEtherealAction() {
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!c.isEthereal) continue;
            this.addToTop(new ExhaustSpecificCardAction(c, AbstractDungeon.player.hand));
        }
        this.isDone = true;
    }
}

