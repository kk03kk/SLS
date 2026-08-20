/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;

public class LiveForeverPower
extends AbstractPower {
    public static final String POWER_ID = "AngelForm";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("AngelForm");

    public LiveForeverPower(AbstractCreature owner, int armorAmt) {
        this.name = LiveForeverPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = armorAmt;
        this.updateDescription();
        this.loadRegion("deva");
    }

    @Override
    public void updateDescription() {
        this.description = LiveForeverPower.powerStrings.DESCRIPTIONS[0] + this.amount + LiveForeverPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.flash();
        this.addToBot(new ApplyPowerAction(this.owner, this.owner, new PlatedArmorPower(this.owner, this.amount), this.amount));
    }
}

