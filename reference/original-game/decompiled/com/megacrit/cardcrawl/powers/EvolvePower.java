/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class EvolvePower
extends AbstractPower {
    public static final String POWER_ID = "Evolve";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Evolve");

    public EvolvePower(AbstractCreature owner, int drawAmt) {
        this.name = EvolvePower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = drawAmt;
        this.updateDescription();
        this.loadRegion("evolve");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? EvolvePower.powerStrings.DESCRIPTIONS[0] : EvolvePower.powerStrings.DESCRIPTIONS[1] + this.amount + EvolvePower.powerStrings.DESCRIPTIONS[2];
    }

    @Override
    public void onCardDraw(AbstractCard card) {
        if (card.type == AbstractCard.CardType.STATUS && !this.owner.hasPower("No Draw")) {
            this.flash();
            this.addToBot(new DrawCardAction(this.owner, this.amount));
        }
    }
}

