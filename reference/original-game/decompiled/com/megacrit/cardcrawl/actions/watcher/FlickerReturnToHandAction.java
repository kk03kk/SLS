/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class FlickerReturnToHandAction
extends AbstractGameAction {
    private AbstractCard card;

    public FlickerReturnToHandAction(AbstractCard card) {
        this.card = card;
        this.duration = Settings.ACTION_DUR_FASTER;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FASTER && AbstractDungeon.player.discardPile.contains(this.card) && AbstractDungeon.player.hand.size() < 10) {
            this.card.returnToHand = true;
        }
        this.tickDuration();
    }
}

