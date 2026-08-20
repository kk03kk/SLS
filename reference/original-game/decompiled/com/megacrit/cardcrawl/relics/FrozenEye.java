/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class FrozenEye
extends AbstractRelic {
    public static final String ID = "Frozen Eye";

    public FrozenEye() {
        super(ID, "frozenEye.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new FrozenEye();
    }
}

