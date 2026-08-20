/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class LizardTail
extends AbstractRelic {
    public static final String ID = "Lizard Tail";

    public LizardTail() {
        super(ID, "lizardTail.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void setCounter(int setCounter) {
        if (setCounter == -2) {
            this.usedUp();
            this.counter = -2;
        }
    }

    @Override
    public void onTrigger() {
        this.flash();
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        int healAmt = AbstractDungeon.player.maxHealth / 2;
        if (healAmt < 1) {
            healAmt = 1;
        }
        AbstractDungeon.player.heal(healAmt, true);
        this.setCounter(-2);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new LizardTail();
    }
}

