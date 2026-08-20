/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class TungstenRod
extends AbstractRelic {
    public static final String ID = "TungstenRod";

    public TungstenRod() {
        super(ID, "tungsten.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public int onLoseHpLast(int damageAmount) {
        if (damageAmount > 0) {
            this.flash();
            return damageAmount - 1;
        }
        return damageAmount;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new TungstenRod();
    }
}

