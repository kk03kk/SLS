/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DevaPower
extends AbstractPower {
    public static final String POWER_ID = "DevaForm";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("DevaForm");
    public static final String NAME = DevaPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DevaPower.powerStrings.DESCRIPTIONS;
    private int energyGainAmount = 1;

    public DevaPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = 1;
        this.energyGainAmount = 1;
        this.updateDescription();
        this.loadRegion("deva2");
    }

    @Override
    public void stackPower(int stackAmount) {
        super.stackPower(stackAmount);
        ++this.energyGainAmount;
    }

    @Override
    public void updateDescription() {
        this.description = this.energyGainAmount == 1 ? DESCRIPTIONS[0] + DESCRIPTIONS[3] + this.amount + DESCRIPTIONS[4] : DESCRIPTIONS[1] + this.energyGainAmount + DESCRIPTIONS[2] + DESCRIPTIONS[3] + this.amount + DESCRIPTIONS[4];
    }

    @Override
    public void onEnergyRecharge() {
        this.flash();
        AbstractDungeon.player.gainEnergy(this.energyGainAmount);
        this.energyGainAmount += this.amount;
        this.updateDescription();
    }
}

