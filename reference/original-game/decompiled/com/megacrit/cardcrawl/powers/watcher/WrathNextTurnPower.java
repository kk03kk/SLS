/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class WrathNextTurnPower
extends AbstractPower {
    public static final String POWER_ID = "WrathNextTurnPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("WrathNextTurnPower");

    public WrathNextTurnPower(AbstractCreature owner) {
        this.name = WrathNextTurnPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("anger");
    }

    @Override
    public void updateDescription() {
        this.description = WrathNextTurnPower.powerStrings.DESCRIPTIONS[0];
    }

    @Override
    public void atStartOfTurn() {
        this.addToBot(new ChangeStanceAction("Wrath"));
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, this));
    }
}

