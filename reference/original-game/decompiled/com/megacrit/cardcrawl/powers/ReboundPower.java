/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ReboundPower
extends AbstractPower {
    public static final String POWER_ID = "Rebound";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Rebound");
    public static final String NAME = ReboundPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ReboundPower.powerStrings.DESCRIPTIONS;
    private boolean justEvoked = true;

    public ReboundPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = 1;
        this.updateDescription();
        this.loadRegion("rebound");
        this.isTurnBased = true;
        this.type = AbstractPower.PowerType.BUFF;
    }

    @Override
    public void updateDescription() {
        this.description = this.amount > 1 ? DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2] : DESCRIPTIONS[0];
    }

    @Override
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        if (this.justEvoked) {
            this.justEvoked = false;
            return;
        }
        if (card.type != AbstractCard.CardType.POWER) {
            this.flash();
            action.reboundCard = true;
        }
        this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        }
    }
}

