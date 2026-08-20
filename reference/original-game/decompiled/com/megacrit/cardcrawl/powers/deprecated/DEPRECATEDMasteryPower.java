/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.stances.AbstractStance;

public class DEPRECATEDMasteryPower
extends AbstractPower {
    public static final String POWER_ID = "Mastery";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Mastery");
    public static final String NAME = DEPRECATEDMasteryPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DEPRECATEDMasteryPower.powerStrings.DESCRIPTIONS;

    public DEPRECATEDMasteryPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("corruption");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void onChangeStance(AbstractStance oldStance, AbstractStance newStance) {
        if (oldStance.ID.equals(newStance.ID) && !newStance.ID.equals("Neutral")) {
            this.flash();
            this.addToBot(new GainEnergyAction(this.amount));
        }
    }
}

