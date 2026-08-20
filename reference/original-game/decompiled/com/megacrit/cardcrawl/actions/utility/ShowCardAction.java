/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ShowCardAction
extends AbstractGameAction {
    private AbstractCard card = null;
    private static final float PURGE_DURATION = 0.2f;

    public ShowCardAction(AbstractCard card) {
        this.setValues(AbstractDungeon.player, null, 1);
        this.card = card;
        this.duration = 0.2f;
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
    }

    @Override
    public void update() {
        if (this.duration == 0.2f) {
            if (AbstractDungeon.player.limbo.contains(this.card)) {
                AbstractDungeon.player.limbo.removeCard(this.card);
            }
            AbstractDungeon.player.cardInUse = null;
        }
        this.tickDuration();
    }
}

