/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class HeatsinkPower
extends AbstractPower {
    public static final String POWER_ID = "Heatsink";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Heatsink");
    public static final String NAME = HeatsinkPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = HeatsinkPower.powerStrings.DESCRIPTIONS;

    public HeatsinkPower(AbstractCreature owner, int drawAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = drawAmt;
        this.updateDescription();
        this.loadRegion("heatsink");
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.POWER) {
            this.flash();
            this.addToTop(new DrawCardAction(this.owner, this.amount));
        }
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
    }

    @Override
    public void updateDescription() {
        this.description = this.amount <= 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[2];
    }
}

