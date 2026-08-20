/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Circlet
extends AbstractRelic {
    public static final String ID = "Circlet";

    public Circlet() {
        super(ID, "circlet.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.CLINK);
        this.counter = 1;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        this.flash();
    }

    @Override
    public void onUnequip() {
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Circlet();
    }
}

