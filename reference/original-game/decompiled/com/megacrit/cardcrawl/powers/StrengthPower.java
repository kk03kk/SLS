/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class StrengthPower
extends AbstractPower {
    public static final String POWER_ID = "Strength";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Strength");
    public static final String NAME = StrengthPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = StrengthPower.powerStrings.DESCRIPTIONS;

    public StrengthPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        if (this.amount >= 999) {
            this.amount = 999;
        }
        if (this.amount <= -999) {
            this.amount = -999;
        }
        this.updateDescription();
        this.loadRegion("strength");
        this.canGoNegative = true;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_STRENGTH", 0.05f);
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        if (this.amount == 0) {
            this.addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        }
        if (this.amount >= 50 && this.owner == AbstractDungeon.player) {
            UnlockTracker.unlockAchievement("JAXXED");
        }
        if (this.amount >= 999) {
            this.amount = 999;
        }
        if (this.amount <= -999) {
            this.amount = -999;
        }
    }

    @Override
    public void reducePower(int reduceAmount) {
        this.fontScale = 8.0f;
        this.amount -= reduceAmount;
        if (this.amount == 0) {
            this.addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, NAME));
        }
        if (this.amount >= 999) {
            this.amount = 999;
        }
        if (this.amount <= -999) {
            this.amount = -999;
        }
    }

    @Override
    public void updateDescription() {
        if (this.amount > 0) {
            this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[2];
            this.type = AbstractPower.PowerType.BUFF;
        } else {
            int tmp = -this.amount;
            this.description = DESCRIPTIONS[1] + tmp + DESCRIPTIONS[2];
            this.type = AbstractPower.PowerType.DEBUFF;
        }
    }

    @Override
    public float atDamageGive(float damage, DamageInfo.DamageType type) {
        if (type == DamageInfo.DamageType.NORMAL) {
            return damage + (float)this.amount;
        }
        return damage;
    }
}

