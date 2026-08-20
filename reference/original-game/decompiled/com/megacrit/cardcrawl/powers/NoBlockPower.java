/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class NoBlockPower
extends AbstractPower {
    public static final String POWER_ID = "NoBlockPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("NoBlockPower");
    public static final String NAME = NoBlockPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = NoBlockPower.powerStrings.DESCRIPTIONS;
    private boolean justApplied = false;

    public NoBlockPower(AbstractCreature owner, int amount, boolean isSourceMonster) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("noBlock");
        if (AbstractDungeon.actionManager.turnHasEnded && isSourceMonster) {
            this.justApplied = true;
        }
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
    }

    @Override
    public void atEndOfRound() {
        if (this.justApplied) {
            this.justApplied = false;
            return;
        }
        if (this.amount == 0) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        } else {
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
        }
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    @Override
    public float modifyBlockLast(float blockAmount) {
        return 0.0f;
    }
}

