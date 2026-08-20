/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class GoldenIdol
extends AbstractRelic {
    public static final String ID = "Golden Idol";
    public static final float MULTIPLIER = 0.25f;

    public GoldenIdol() {
        super(ID, "goldenIdolRelic.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new GoldenIdol();
    }
}

