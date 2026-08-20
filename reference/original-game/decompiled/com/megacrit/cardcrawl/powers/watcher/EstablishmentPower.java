/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.unique.EstablishmentPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class EstablishmentPower
extends AbstractPower {
    public static final String POWER_ID = "EstablishmentPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("EstablishmentPower");

    public EstablishmentPower(AbstractCreature owner, int strengthAmount) {
        this.name = EstablishmentPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = strengthAmount;
        this.updateDescription();
        this.loadRegion("establishment");
        this.priority = 25;
    }

    @Override
    public void updateDescription() {
        this.description = EstablishmentPower.powerStrings.DESCRIPTIONS[0] + this.amount + EstablishmentPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.flash();
        this.addToBot(new EstablishmentPowerAction(this.amount));
    }
}

