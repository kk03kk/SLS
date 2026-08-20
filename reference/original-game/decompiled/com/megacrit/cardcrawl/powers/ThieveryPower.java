/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ThieveryPower
extends AbstractPower {
    public static final String POWER_ID = "Thievery";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Thievery");
    public static final String NAME = ThieveryPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ThieveryPower.powerStrings.DESCRIPTIONS;

    public ThieveryPower(AbstractCreature owner, int stealAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = stealAmount;
        this.updateDescription();
        this.loadRegion("thievery");
    }

    @Override
    public void updateDescription() {
        this.description = this.owner.name + DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

