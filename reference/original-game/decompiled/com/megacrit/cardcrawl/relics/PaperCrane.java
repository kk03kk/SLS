/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class PaperCrane
extends AbstractRelic {
    public static final String ID = "Paper Crane";
    public static final float WEAK_EFFECTIVENESS = 0.6f;
    public static final int EFFECTIVENESS_STRING = 40;

    public PaperCrane() {
        super(ID, "paperCrane.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PaperCrane();
    }
}

