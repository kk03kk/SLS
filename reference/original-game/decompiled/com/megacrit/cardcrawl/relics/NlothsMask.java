/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class NlothsMask
extends AbstractRelic {
    public static final String ID = "NlothsMask";

    public NlothsMask() {
        super(ID, "nloth.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.FLAT);
        this.counter = 1;
    }

    @Override
    public void onChestOpenAfter(boolean bossChest) {
        if (!bossChest && this.counter > 0) {
            --this.counter;
            this.flash();
            AbstractDungeon.getCurrRoom().removeOneRelicFromRewards();
            if (this.counter == 0) {
                this.setCounter(-2);
            }
        }
    }

    @Override
    public void setCounter(int setCounter) {
        this.counter = setCounter;
        if (setCounter == -2) {
            this.usedUp();
            this.counter = -2;
        }
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new NlothsMask();
    }
}

