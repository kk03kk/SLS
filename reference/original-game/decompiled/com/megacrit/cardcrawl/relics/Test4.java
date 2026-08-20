/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Test4
extends AbstractRelic {
    public static final String ID = "Test 4";

    public Test4() {
        super(ID, "test4.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Test4();
    }
}

