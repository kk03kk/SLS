/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class MarkOfTheBloom
extends AbstractRelic {
    public static final String ID = "Mark of the Bloom";

    public MarkOfTheBloom() {
        super(ID, "bloom.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public int onPlayerHeal(int healAmount) {
        this.flash();
        return 0;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new MarkOfTheBloom();
    }
}

