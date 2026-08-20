/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class ChemicalX
extends AbstractRelic {
    public static final String ID = "Chemical X";
    public static final int BOOST = 2;

    public ChemicalX() {
        super(ID, "chemicalX.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        if (!this.DESCRIPTIONS[1].equals("")) {
            return this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[1];
        }
        return this.DESCRIPTIONS[0] + 2 + LocalizedStrings.PERIOD;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new ChemicalX();
    }
}

