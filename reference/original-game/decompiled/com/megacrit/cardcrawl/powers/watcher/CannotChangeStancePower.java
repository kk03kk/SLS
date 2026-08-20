/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class CannotChangeStancePower
extends AbstractPower {
    public static final String POWER_ID = "CannotChangeStancePower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("CannotChangeStancePower");

    public CannotChangeStancePower(AbstractCreature owner) {
        this.name = CannotChangeStancePower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("no_stance");
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        }
    }

    @Override
    public void updateDescription() {
        this.description = CannotChangeStancePower.powerStrings.DESCRIPTIONS[0];
    }
}

