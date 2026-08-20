/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class RechargingCorePower
extends AbstractPower {
    public static final String POWER_ID = "RechargingCore";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("RechargingCore");
    public static final String NAME = RechargingCorePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = RechargingCorePower.powerStrings.DESCRIPTIONS;
    private int turnTimer;

    public RechargingCorePower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.turnTimer = 3;
        this.updateDescription();
        this.loadRegion("conserve");
        this.type = AbstractPower.PowerType.BUFF;
        this.isTurnBased = true;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.turnTimer;
        this.description = this.turnTimer == 1 ? this.description + DESCRIPTIONS[1] : this.description + DESCRIPTIONS[2];
        for (int i = 0; i < this.amount; ++i) {
            this.description = this.description + DESCRIPTIONS[3];
        }
        this.description = this.description + " .";
    }

    @Override
    public void atStartOfTurn() {
        this.updateDescription();
        if (this.turnTimer == 1) {
            this.flash();
            this.turnTimer = 3;
            this.addToBot(new GainEnergyAction(this.amount));
        } else {
            --this.turnTimer;
        }
        this.updateDescription();
    }
}

