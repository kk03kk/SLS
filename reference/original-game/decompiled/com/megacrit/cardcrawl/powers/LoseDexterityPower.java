/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;

public class LoseDexterityPower
extends AbstractPower {
    public static final String POWER_ID = "DexLoss";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("DexLoss");

    public LoseDexterityPower(AbstractCreature owner, int newAmount) {
        this.name = LoseDexterityPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.type = AbstractPower.PowerType.DEBUFF;
        this.updateDescription();
        this.loadRegion("flex");
    }

    @Override
    public void updateDescription() {
        this.description = LoseDexterityPower.powerStrings.DESCRIPTIONS[0] + this.amount + LoseDexterityPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.flash();
        this.addToBot(new ApplyPowerAction(this.owner, this.owner, new DexterityPower(this.owner, -this.amount), -this.amount));
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
    }
}

