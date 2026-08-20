/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class IntangiblePower
extends AbstractPower {
    public static final String POWER_ID = "Intangible";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Intangible");
    public static final String NAME = IntangiblePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = IntangiblePower.powerStrings.DESCRIPTIONS;
    private boolean justApplied;

    public IntangiblePower(AbstractCreature owner, int turns) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = turns;
        this.updateDescription();
        this.loadRegion("intangible");
        this.priority = 75;
        this.justApplied = true;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_INTANGIBLE", 0.05f);
    }

    @Override
    public float atDamageFinalReceive(float damage, DamageInfo.DamageType type) {
        if (damage > 1.0f) {
            damage = 1.0f;
        }
        return damage;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (this.justApplied) {
            this.justApplied = false;
            return;
        }
        this.flash();
        if (this.amount == 0) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        } else {
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
        }
    }
}

