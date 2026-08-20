/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DoubleDamagePower;

public class PhantasmalPower
extends AbstractPower {
    public static final String POWER_ID = "Phantasmal";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Phantasmal");
    public static final String NAME = PhantasmalPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = PhantasmalPower.powerStrings.DESCRIPTIONS;

    public PhantasmalPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("phantasmal");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] : DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }

    @Override
    public void atStartOfTurn() {
        this.flash();
        this.addToBot(new ApplyPowerAction(this.owner, this.owner, new DoubleDamagePower(this.owner, 1, false), this.amount));
        this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
    }
}

