/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class FrailPower
extends AbstractPower {
    public static final String POWER_ID = "Frail";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Frail");
    public static final String NAME = FrailPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = FrailPower.powerStrings.DESCRIPTIONS;
    private boolean justApplied = false;

    public FrailPower(AbstractCreature owner, int amount, boolean isSourceMonster) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.priority = 10;
        this.updateDescription();
        this.loadRegion("frail");
        if (isSourceMonster) {
            this.justApplied = true;
        }
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
    }

    @Override
    public void atEndOfRound() {
        if (this.justApplied) {
            this.justApplied = false;
            return;
        }
        if (this.amount == 0) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        } else {
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
        }
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[2];
    }

    @Override
    public float modifyBlock(float blockAmount) {
        return blockAmount * 0.75f;
    }
}

