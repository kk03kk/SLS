/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class InvinciblePower
extends AbstractPower {
    public static final String POWER_ID = "Invincible";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Invincible");
    public static final String NAME = InvinciblePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = InvinciblePower.powerStrings.DESCRIPTIONS;
    private int maxAmt;

    public InvinciblePower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.maxAmt = amount;
        this.updateDescription();
        this.loadRegion("heartDef");
        this.priority = 99;
    }

    @Override
    public int onAttackedToChangeDamage(DamageInfo info, int damageAmount) {
        if (damageAmount > this.amount) {
            damageAmount = this.amount;
        }
        this.amount -= damageAmount;
        if (this.amount < 0) {
            this.amount = 0;
        }
        this.updateDescription();
        return damageAmount;
    }

    @Override
    public void atStartOfTurn() {
        this.amount = this.maxAmt;
        this.updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = this.amount <= 0 ? DESCRIPTIONS[2] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

