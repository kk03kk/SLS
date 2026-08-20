/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class ExhaustAllNonAttackAction
extends AbstractGameAction {
    private float startingDuration;

    public ExhaustAllNonAttackAction() {
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.duration = this.startingDuration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                if (c.type == AbstractCard.CardType.ATTACK) continue;
                this.addToTop(new ExhaustSpecificCardAction(c, AbstractDungeon.player.hand));
            }
            this.isDone = true;
            if (AbstractDungeon.player.exhaustPile.size() >= 20) {
                UnlockTracker.unlockAchievement("THE_PACT");
            }
        }
    }
}

