/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class FocusPower
extends AbstractPower {
    public static final String POWER_ID = "Focus";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Focus");
    public static final String NAME = FocusPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = FocusPower.powerStrings.DESCRIPTIONS;

    public FocusPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("focus");
        this.canGoNegative = true;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_FOCUS", 0.05f);
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        if (this.amount == 0) {
            this.addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        }
        if (this.amount >= 25) {
            UnlockTracker.unlockAchievement("FOCUSED");
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
}

