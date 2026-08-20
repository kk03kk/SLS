/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class MillAction
extends AbstractGameAction {
    public MillAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.setValues(target, source, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            if (this.amount <= AbstractDungeon.player.drawPile.size()) {
                for (int i = 0; i < this.amount; ++i) {
                    AbstractDungeon.player.drawPile.moveToDiscardPile(AbstractDungeon.player.drawPile.getTopCard());
                }
            } else {
                for (int i = 0; i < AbstractDungeon.player.drawPile.size(); ++i) {
                    AbstractDungeon.player.drawPile.moveToDiscardPile(AbstractDungeon.player.drawPile.getTopCard());
                }
            }
        }
        this.tickDuration();
    }
}

