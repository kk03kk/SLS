/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class SpiritPoop
extends AbstractRelic {
    public static final String ID = "Spirit Poop";

    public SpiritPoop() {
        super(ID, "spiritPoop.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new SpiritPoop();
    }
}

