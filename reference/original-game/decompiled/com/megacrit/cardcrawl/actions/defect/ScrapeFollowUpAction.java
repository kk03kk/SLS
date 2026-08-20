/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ScrapeFollowUpAction
extends AbstractGameAction {
    public ScrapeFollowUpAction() {
        this.duration = 0.001f;
    }

    @Override
    public void update() {
        AbstractDungeon.actionManager.addToTop(new WaitAction(0.4f));
        this.tickDuration();
        if (this.isDone) {
            for (AbstractCard c : DrawCardAction.drawnCards) {
                if (c.costForTurn == 0 || c.freeToPlayOnce) continue;
                AbstractDungeon.player.hand.moveToDiscardPile(c);
                c.triggerOnManualDiscard();
                GameActionManager.incrementDiscard(false);
            }
        }
    }
}

