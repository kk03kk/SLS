/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Test3
extends AbstractRelic {
    public static final String ID = "Test 3";

    public Test3() {
        super(ID, "test3.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Test3();
    }
}

