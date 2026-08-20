/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class BerserkPower
extends AbstractPower {
    public static final String POWER_ID = "Berserk";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Berserk");

    public BerserkPower(AbstractCreature owner, int amount) {
        this.name = BerserkPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("berserk");
    }

    @Override
    public void updateDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(BerserkPower.powerStrings.DESCRIPTIONS[0]);
        for (int i = 0; i < this.amount; ++i) {
            sb.append("[R] ");
        }
        sb.append(LocalizedStrings.PERIOD);
        this.description = sb.toString();
    }

    @Override
    public void atStartOfTurn() {
        this.addToBot(new GainEnergyAction(this.amount));
        this.flash();
    }
}

