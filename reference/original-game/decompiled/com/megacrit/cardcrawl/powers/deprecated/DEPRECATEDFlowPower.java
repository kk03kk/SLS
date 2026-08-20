/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.LoseDexterityPower;
import com.megacrit.cardcrawl.powers.LoseStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class DEPRECATEDFlowPower
extends AbstractPower {
    public static final String POWER_ID = "FlowPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("FlowPower");

    public DEPRECATEDFlowPower(AbstractCreature owner, int amount) {
        this.name = DEPRECATEDFlowPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("afterImage");
    }

    @Override
    public void updateDescription() {
        this.description = DEPRECATEDFlowPower.powerStrings.DESCRIPTIONS[0] + this.amount + DEPRECATEDFlowPower.powerStrings.DESCRIPTIONS[1] + this.amount + DEPRECATEDFlowPower.powerStrings.DESCRIPTIONS[2];
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.SKILL) {
            this.flash();
            this.addToBot(new ApplyPowerAction(this.owner, this.owner, new StrengthPower(this.owner, this.amount), this.amount));
            this.addToBot(new ApplyPowerAction(this.owner, this.owner, new LoseStrengthPower(this.owner, this.amount), this.amount));
        } else if (card.type == AbstractCard.CardType.ATTACK) {
            this.flash();
            this.addToBot(new ApplyPowerAction(this.owner, this.owner, new DexterityPower(this.owner, this.amount), this.amount));
            this.addToBot(new ApplyPowerAction(this.owner, this.owner, new LoseDexterityPower(this.owner, this.amount), this.amount));
        }
    }
}

