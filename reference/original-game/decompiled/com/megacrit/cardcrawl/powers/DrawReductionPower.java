/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DrawReductionPower
extends AbstractPower {
    public static final String POWER_ID = "Draw Reduction";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Draw Reduction");
    public static final String NAME = DrawReductionPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DrawReductionPower.powerStrings.DESCRIPTIONS;
    private boolean justApplied = true;

    public DrawReductionPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("lessdraw");
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
    }

    @Override
    public void onInitialApplication() {
        --AbstractDungeon.player.gameHandSize;
    }

    @Override
    public void atEndOfRound() {
        if (this.justApplied) {
            this.justApplied = false;
            return;
        }
        this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
    }

    @Override
    public void onRemove() {
        ++AbstractDungeon.player.gameHandSize;
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] : DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }
}

