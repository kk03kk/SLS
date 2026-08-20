/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;

public class UpgradeSpecificCardAction
extends AbstractGameAction {
    private AbstractCard c;

    public UpgradeSpecificCardAction(AbstractCard cardToUpgrade) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.c = cardToUpgrade;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.c.canUpgrade() && this.c.type != AbstractCard.CardType.STATUS) {
                this.c.upgrade();
                this.c.superFlash();
                this.c.applyPowers();
            }
            this.isDone = true;
            return;
        }
        this.tickDuration();
    }
}

