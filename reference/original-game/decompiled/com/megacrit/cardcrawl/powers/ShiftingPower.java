/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.GainStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class ShiftingPower
extends AbstractPower {
    public static final String POWER_ID = "Shifting";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Shifting");
    public static final String NAME = ShiftingPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ShiftingPower.powerStrings.DESCRIPTIONS;

    public ShiftingPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.isPostActionPower = true;
        this.loadRegion("shift");
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (damageAmount > 0) {
            this.addToTop(new ApplyPowerAction(this.owner, this.owner, new StrengthPower(this.owner, -damageAmount), -damageAmount));
            if (!this.owner.hasPower("Artifact")) {
                this.addToTop(new ApplyPowerAction(this.owner, this.owner, new GainStrengthPower(this.owner, damageAmount), damageAmount));
            }
            this.flash();
        }
        return damageAmount;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[1];
    }
}

