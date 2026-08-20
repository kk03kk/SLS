/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.watcher.VigorPower;

public class DEPRECATEDRetributionPower
extends AbstractPower {
    public static final String POWER_ID = "Retribution";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Retribution");

    public DEPRECATEDRetributionPower(AbstractCreature owner, int vigorAmt) {
        this.name = DEPRECATEDRetributionPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = vigorAmt;
        this.updateDescription();
        this.loadRegion("anger");
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (damageAmount > 0) {
            this.flash();
            this.addToTop(new ApplyPowerAction(this.owner, this.owner, new VigorPower(this.owner, this.amount), this.amount));
        }
        return damageAmount;
    }

    @Override
    public void updateDescription() {
        this.description = DEPRECATEDRetributionPower.powerStrings.DESCRIPTIONS[0] + this.amount + DEPRECATEDRetributionPower.powerStrings.DESCRIPTIONS[1];
    }
}

