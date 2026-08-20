/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Calipers
extends AbstractRelic {
    public static final String ID = "Calipers";
    public static final int BLOCK_LOSS = 15;

    public Calipers() {
        super(ID, "calipers.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 15 + this.DESCRIPTIONS[1];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Calipers();
    }
}

