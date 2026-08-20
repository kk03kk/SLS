/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class CorruptionPower
extends AbstractPower {
    public static final String POWER_ID = "Corruption";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Corruption");
    public static final String NAME = CorruptionPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = CorruptionPower.powerStrings.DESCRIPTIONS;

    public CorruptionPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.description = DESCRIPTIONS[0];
        this.loadRegion("corruption");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[1];
    }

    @Override
    public void onCardDraw(AbstractCard card) {
        if (card.type == AbstractCard.CardType.SKILL) {
            card.setCostForTurn(-9);
        }
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.SKILL) {
            this.flash();
            action.exhaustCard = true;
        }
    }
}

