/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class BladeFuryAction
extends AbstractGameAction {
    private boolean upgrade;

    public BladeFuryAction(boolean upgraded) {
        this.upgrade = upgraded;
    }

    @Override
    public void update() {
        int theSize = AbstractDungeon.player.hand.size();
        if (this.upgrade) {
            AbstractCard s = new Shiv().makeCopy();
            s.upgrade();
            this.addToTop(new MakeTempCardInHandAction(s, theSize));
        } else {
            this.addToTop(new MakeTempCardInHandAction((AbstractCard)new Shiv(), theSize));
        }
        this.addToTop(new DiscardAction(AbstractDungeon.player, AbstractDungeon.player, theSize, false));
        this.isDone = true;
    }
}

