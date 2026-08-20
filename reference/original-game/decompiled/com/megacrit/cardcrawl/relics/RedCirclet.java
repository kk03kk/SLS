/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class RedCirclet
extends AbstractRelic {
    public static final String ID = "Red Circlet";

    public RedCirclet() {
        super(ID, "redCirclet.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new RedCirclet();
    }
}

