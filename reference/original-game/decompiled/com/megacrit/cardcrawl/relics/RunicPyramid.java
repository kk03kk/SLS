/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class RunicPyramid
extends AbstractRelic {
    public static final String ID = "Runic Pyramid";

    public RunicPyramid() {
        super(ID, "runicPyramid.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new RunicPyramid();
    }
}

