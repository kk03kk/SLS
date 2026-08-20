/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDGroundedPower
extends AbstractPower {
    public static final String POWER_ID = "Grounded";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Grounded");
    public static final String NAME = DEPRECATEDGroundedPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DEPRECATEDGroundedPower.powerStrings.DESCRIPTIONS;

    public DEPRECATEDGroundedPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("corruption");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.SKILL) {
            this.flash();
            this.addToBot(new ChangeStanceAction("Calm"));
        }
    }
}

