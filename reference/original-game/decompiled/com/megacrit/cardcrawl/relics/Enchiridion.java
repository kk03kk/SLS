/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class Enchiridion
extends AbstractRelic {
    public static final String ID = "Enchiridion";

    public Enchiridion() {
        super(ID, "enchiridion.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atPreBattle() {
        this.flash();
        AbstractCard c = AbstractDungeon.returnTrulyRandomCardInCombat(AbstractCard.CardType.POWER).makeCopy();
        if (c.cost != -1) {
            c.setCostForTurn(0);
        }
        UnlockTracker.markCardAsSeen(c.cardID);
        this.addToBot(new MakeTempCardInHandAction(c));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Enchiridion();
    }
}

