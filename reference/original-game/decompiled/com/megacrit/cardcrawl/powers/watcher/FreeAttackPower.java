/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class FreeAttackPower
extends AbstractPower {
    public static final String POWER_ID = "FreeAttackPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("FreeAttackPower");

    public FreeAttackPower(AbstractCreature owner, int amount) {
        this.name = FreeAttackPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("swivel");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? FreeAttackPower.powerStrings.DESCRIPTIONS[0] : FreeAttackPower.powerStrings.DESCRIPTIONS[1] + this.amount + FreeAttackPower.powerStrings.DESCRIPTIONS[2];
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.ATTACK && !card.purgeOnUse && this.amount > 0) {
            this.flash();
            --this.amount;
            if (this.amount == 0) {
                this.addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
            }
        }
    }
}

