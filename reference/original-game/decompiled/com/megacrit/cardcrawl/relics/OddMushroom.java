/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class OddMushroom
extends AbstractRelic {
    public static final String ID = "Odd Mushroom";
    public static final float VULN_EFFECTIVENESS = 1.25f;
    public static final int EFFECTIVENESS_STRING = 25;

    public OddMushroom() {
        super(ID, "mushroom.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new OddMushroom();
    }
}

