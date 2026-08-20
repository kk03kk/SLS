/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ExpertiseAction
extends AbstractGameAction {
    public ExpertiseAction(AbstractCreature source, int amount) {
        this.setValues(this.target, source, amount);
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        int toDraw = this.amount - AbstractDungeon.player.hand.size();
        if (toDraw > 0) {
            this.addToTop(new DrawCardAction(this.source, toDraw));
        }
        this.isDone = true;
    }
}

