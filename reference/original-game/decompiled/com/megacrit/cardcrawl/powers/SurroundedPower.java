/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class SurroundedPower
extends AbstractPower {
    public static final String POWER_ID = "Surrounded";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Surrounded");

    public SurroundedPower(AbstractCreature owner) {
        this.name = SurroundedPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("surrounded");
    }

    @Override
    public void updateDescription() {
        this.description = SurroundedPower.powerStrings.DESCRIPTIONS[0];
    }
}

