/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDCondensePower
extends AbstractPower {
    public static final String POWER_ID = "DEPRECATEDCondense";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("DEPRECATEDCondense");
    public static final String NAME = DEPRECATEDCondensePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DEPRECATEDCondensePower.powerStrings.DESCRIPTIONS;

    public DEPRECATEDCondensePower(AbstractCreature owner, int bufferAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = bufferAmt;
        this.updateDescription();
        this.loadRegion("buffer");
    }

    @Override
    public int onLoseHp(int damageAmount) {
        if (damageAmount > this.amount) {
            this.flash();
            return this.amount;
        }
        return damageAmount;
    }

    @Override
    public void stackPower(int stackAmount) {
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

