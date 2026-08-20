/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class BrutalityPower
extends AbstractPower {
    public static final String POWER_ID = "Brutality";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Brutality");
    public static final String NAME = BrutalityPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = BrutalityPower.powerStrings.DESCRIPTIONS;

    public BrutalityPower(AbstractCreature owner, int drawAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = drawAmount;
        this.updateDescription();
        this.loadRegion("brutality");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2] : DESCRIPTIONS[3] + this.amount + DESCRIPTIONS[4] + this.amount + DESCRIPTIONS[5];
    }

    @Override
    public void atStartOfTurnPostDraw() {
        this.flash();
        this.addToBot(new DrawCardAction(this.owner, this.amount));
        this.addToBot(new LoseHPAction(this.owner, this.owner, this.amount));
    }
}

