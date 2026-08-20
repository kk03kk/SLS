/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DrawCardNextTurnPower
extends AbstractPower {
    public static final String POWER_ID = "Draw Card";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Draw Card");
    public static final String NAME = DrawCardNextTurnPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DrawCardNextTurnPower.powerStrings.DESCRIPTIONS;

    public DrawCardNextTurnPower(AbstractCreature owner, int drawAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = drawAmount;
        this.updateDescription();
        this.loadRegion("carddraw");
        this.priority = 20;
    }

    @Override
    public void updateDescription() {
        this.description = this.amount > 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[2];
    }

    @Override
    public void atStartOfTurnPostDraw() {
        this.flash();
        this.addToBot(new DrawCardAction(this.owner, this.amount));
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
    }
}

