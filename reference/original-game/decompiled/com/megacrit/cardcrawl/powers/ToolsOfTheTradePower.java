/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.DiscardAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ToolsOfTheTradePower
extends AbstractPower {
    public static final String POWER_ID = "Tools Of The Trade";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Tools Of The Trade");
    public static final String NAME = ToolsOfTheTradePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ToolsOfTheTradePower.powerStrings.DESCRIPTIONS;

    public ToolsOfTheTradePower(AbstractCreature owner, int drawAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = drawAmount;
        this.updateDescription();
        this.loadRegion("tools");
        this.priority = 25;
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[3] + this.amount + DESCRIPTIONS[4];
    }

    @Override
    public void atStartOfTurnPostDraw() {
        this.flash();
        this.addToBot(new DrawCardAction(this.owner, this.amount));
        this.addToBot(new DiscardAction(this.owner, this.owner, this.amount, false));
    }
}

