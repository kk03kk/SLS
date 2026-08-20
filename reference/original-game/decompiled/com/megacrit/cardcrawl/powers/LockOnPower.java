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

public class LockOnPower
extends AbstractPower {
    public static final String POWER_ID = "Lockon";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Lockon");
    public static final String NAME = LockOnPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = LockOnPower.powerStrings.DESCRIPTIONS;
    public static final float MULTIPLIER = 1.5f;
    private static final int MULTI_STR = 50;

    public LockOnPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("lockon");
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
    }

    @Override
    public void atEndOfRound() {
        if (this.amount == 0) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        } else {
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
        }
    }

    @Override
    public void updateDescription() {
        if (this.owner != null) {
            this.description = this.amount == 1 ? DESCRIPTIONS[0] + 50 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2] : DESCRIPTIONS[0] + 50 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[3];
        }
    }
}

