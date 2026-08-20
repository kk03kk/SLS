/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class SlowPower
extends AbstractPower {
    public static final String POWER_ID = "Slow";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Slow");
    public static final String NAME = SlowPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = SlowPower.powerStrings.DESCRIPTIONS;

    public SlowPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("slow");
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void atEndOfRound() {
        this.amount = 0;
        this.updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + FontHelper.colorString(this.owner.name, "y") + DESCRIPTIONS[1];
        if (this.amount != 0) {
            this.description = this.description + DESCRIPTIONS[2] + this.amount * 10 + DESCRIPTIONS[3];
        }
    }

    @Override
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        this.addToBot(new ApplyPowerAction(this.owner, this.owner, new SlowPower(this.owner, 1), 1));
    }

    @Override
    public float atDamageReceive(float damage, DamageInfo.DamageType type) {
        if (type == DamageInfo.DamageType.NORMAL) {
            return damage * (1.0f + (float)this.amount * 0.1f);
        }
        return damage;
    }
}

