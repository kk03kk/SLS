/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class NlothsGift
extends AbstractRelic {
    public static final String ID = "Nloth's Gift";
    public static final float MULTIPLIER = 3.0f;

    public NlothsGift() {
        super(ID, "nlothsGift.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public int changeRareCardRewardChance(int rareCardChance) {
        return rareCardChance * 3;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new NlothsGift();
    }
}

