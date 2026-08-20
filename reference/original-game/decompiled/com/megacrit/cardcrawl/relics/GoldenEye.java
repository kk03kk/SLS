/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class GoldenEye
extends AbstractRelic {
    public static final String ID = "GoldenEye";

    public GoldenEye() {
        super(ID, "golden_eye.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[1];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new GoldenEye();
    }
}

