/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.stances.AbstractStance;

public class VioletLotus
extends AbstractRelic {
    public static final String ID = "VioletLotus";

    public VioletLotus() {
        super(ID, "violet_lotus.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onChangeStance(AbstractStance prevStance, AbstractStance newStance) {
        if (!prevStance.ID.equals(newStance.ID) && prevStance.ID.equals("Calm")) {
            this.flash();
            this.addToBot(new GainEnergyAction(1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new VioletLotus();
    }
}

