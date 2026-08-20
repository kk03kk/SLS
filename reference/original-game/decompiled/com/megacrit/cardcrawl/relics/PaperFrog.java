/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class PaperFrog
extends AbstractRelic {
    public static final String ID = "Paper Frog";
    public static final float VULN_EFFECTIVENESS = 1.75f;
    public static final int EFFECTIVENESS_STRING = 75;

    public PaperFrog() {
        super(ID, "paperFrog.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PaperFrog();
    }
}

