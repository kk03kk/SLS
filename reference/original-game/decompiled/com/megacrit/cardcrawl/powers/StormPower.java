/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class StormPower
extends AbstractPower {
    public static final String POWER_ID = "Storm";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Storm");
    public static final String NAME = StormPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = StormPower.powerStrings.DESCRIPTIONS;

    public StormPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("storm");
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.POWER && this.amount > 0) {
            this.flash();
            for (int i = 0; i < this.amount; ++i) {
                this.addToBot(new ChannelAction(new Lightning()));
            }
        }
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

