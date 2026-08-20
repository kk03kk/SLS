/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.stances.AbstractStance;

public class MentalFortressPower
extends AbstractPower {
    public static final String POWER_ID = "Controlled";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Controlled");

    public MentalFortressPower(AbstractCreature owner, int amount) {
        this.name = MentalFortressPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("mental_fortress");
    }

    @Override
    public void updateDescription() {
        this.description = MentalFortressPower.powerStrings.DESCRIPTIONS[0] + this.amount + MentalFortressPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void onChangeStance(AbstractStance oldStance, AbstractStance newStance) {
        if (!oldStance.ID.equals(newStance.ID)) {
            this.flash();
            this.addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
        }
    }
}

