/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.unique.LoseEnergyAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class EnergyDownPower
extends AbstractPower {
    public static final String POWER_ID = "EnergyDownPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("EnergyDownPower");

    public EnergyDownPower(AbstractCreature owner, int amount, boolean isFasting) {
        this.name = EnergyDownPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        if (isFasting) {
            this.loadRegion("fasting");
        } else {
            this.loadRegion("energized_blue");
        }
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    public EnergyDownPower(AbstractCreature owner, int amount) {
        this(owner, amount, false);
    }

    @Override
    public void updateDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(EnergyDownPower.powerStrings.DESCRIPTIONS[0]);
        for (int i = 0; i < this.amount; ++i) {
            sb.append("[E] ");
        }
        if (EnergyDownPower.powerStrings.DESCRIPTIONS[1].isEmpty()) {
            sb.append(LocalizedStrings.PERIOD);
        } else {
            sb.append(EnergyDownPower.powerStrings.DESCRIPTIONS[1]);
        }
        this.description = sb.toString();
    }

    @Override
    public void atStartOfTurn() {
        this.addToBot(new LoseEnergyAction(this.amount));
        this.flash();
    }
}

