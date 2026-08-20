/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ModeShiftPower
extends AbstractPower {
    public static final String POWER_ID = "Mode Shift";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Mode Shift");
    public static final String NAME = ModeShiftPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ModeShiftPower.powerStrings.DESCRIPTIONS;

    public ModeShiftPower(AbstractCreature owner, int newAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.updateDescription();
        this.loadRegion("modeShift");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

