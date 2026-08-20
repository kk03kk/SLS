/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class InkBottle
extends AbstractRelic {
    public static final String ID = "InkBottle";
    private static final int COUNT = 10;

    public InkBottle() {
        super(ID, "ink_bottle.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.CLINK);
        this.counter = 0;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        ++this.counter;
        if (this.counter == 10) {
            this.counter = 0;
            this.flash();
            this.pulse = false;
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new DrawCardAction(1));
        } else if (this.counter == 9) {
            this.beginPulse();
            this.pulse = true;
        }
    }

    @Override
    public void atBattleStart() {
        if (this.counter == 9) {
            this.beginPulse();
            this.pulse = true;
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new InkBottle();
    }
}

