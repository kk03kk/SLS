/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class UnawakenedPower
extends AbstractPower {
    public static final String POWER_ID = "Unawakened";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Unawakened");
    public static final String NAME = UnawakenedPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = UnawakenedPower.powerStrings.DESCRIPTIONS;

    public UnawakenedPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("unawakened");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}

