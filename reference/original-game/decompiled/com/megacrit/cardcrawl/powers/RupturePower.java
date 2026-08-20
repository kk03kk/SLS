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
import com.megacrit.cardcrawl.powers.StrengthPower;

public class RupturePower
extends AbstractPower {
    public static final String POWER_ID = "Rupture";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Rupture");
    public static final String NAME = RupturePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = RupturePower.powerStrings.DESCRIPTIONS;

    public RupturePower(AbstractCreature owner, int strAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = strAmt;
        this.updateDescription();
        this.isPostActionPower = true;
        this.loadRegion("rupture");
    }

    @Override
    public void wasHPLost(DamageInfo info, int damageAmount) {
        if (damageAmount > 0 && info.owner == this.owner) {
            this.flash();
            this.addToTop(new ApplyPowerAction(this.owner, this.owner, new StrengthPower(this.owner, this.amount), this.amount));
        }
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

