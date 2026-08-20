/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class WhiteBeast
extends AbstractRelic {
    public static final String ID = "White Beast Statue";

    public WhiteBeast() {
        super(ID, "whiteBeast.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new WhiteBeast();
    }
}

