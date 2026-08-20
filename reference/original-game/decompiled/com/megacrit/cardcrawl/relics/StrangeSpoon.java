/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class StrangeSpoon
extends AbstractRelic {
    public static final String ID = "Strange Spoon";
    public static final int DISCARD_CHANCE = 50;

    public StrangeSpoon() {
        super(ID, "bigSpoon.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new StrangeSpoon();
    }
}

