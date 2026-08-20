/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class LightningMasteryPower
extends AbstractPower {
    public static final String POWER_ID = "Lightning Mastery";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Lightning Mastery");
    public static final String NAME = LightningMasteryPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = LightningMasteryPower.powerStrings.DESCRIPTIONS;

    public LightningMasteryPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("mastery");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

