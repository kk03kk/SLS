/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class SkillBurnPower
extends AbstractPower {
    public static final String POWER_ID = "Skill Burn";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Skill Burn");
    public static final String NAME = SkillBurnPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = SkillBurnPower.powerStrings.DESCRIPTIONS;
    private boolean justApplied = true;

    public SkillBurnPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("skillBurn");
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
    }

    @Override
    public void atEndOfRound() {
        if (this.justApplied) {
            this.justApplied = false;
            return;
        }
        this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] : DESCRIPTIONS[2] + this.amount + DESCRIPTIONS[3];
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.SKILL) {
            this.flash();
            action.exhaustCard = true;
        }
    }
}

