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
import com.megacrit.cardcrawl.powers.StrengthPower;

public class LoseStrengthPower
extends AbstractPower {
    public static final String POWER_ID = "Flex";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Flex");
    public static final String NAME = LoseStrengthPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = LoseStrengthPower.powerStrings.DESCRIPTIONS;

    public LoseStrengthPower(AbstractCreature owner, int newAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.type = AbstractPower.PowerType.DEBUFF;
        this.updateDescription();
        this.loadRegion("flex");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.flash();
        this.addToBot(new ApplyPowerAction(this.owner, this.owner, new StrengthPower(this.owner, -this.amount), -this.amount));
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
    }
}

