/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class GenericStrengthUpPower
extends AbstractPower {
    public static final String POWER_ID = "Generic Strength Up Power";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Generic Strength Up Power");
    public static final String NAME = GenericStrengthUpPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = GenericStrengthUpPower.powerStrings.DESCRIPTIONS;

    public GenericStrengthUpPower(AbstractCreature owner, String newName, int strAmt) {
        this.name = newName;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = strAmt;
        this.updateDescription();
        this.loadRegion("stasis");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfRound() {
        this.flash();
        this.addToBot(new ApplyPowerAction(this.owner, this.owner, new StrengthPower(this.owner, this.amount), this.amount));
    }
}

