/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class WeakPower
extends AbstractPower {
    public static final String POWER_ID = "Weakened";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Weakened");
    public static final String NAME = WeakPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = WeakPower.powerStrings.DESCRIPTIONS;
    private boolean justApplied = false;
    private static final int EFFECTIVENESS_STRING = 25;

    public WeakPower(AbstractCreature owner, int amount, boolean isSourceMonster) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("weak");
        if (isSourceMonster) {
            this.justApplied = true;
        }
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
        this.priority = 99;
    }

    @Override
    public void atEndOfRound() {
        if (this.justApplied) {
            this.justApplied = false;
            return;
        }
        if (this.amount == 0) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        } else {
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
        }
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? (this.owner != null && !this.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Crane") ? DESCRIPTIONS[0] + 40 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2] : DESCRIPTIONS[0] + 25 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2]) : (this.owner != null && !this.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Crane") ? DESCRIPTIONS[0] + 40 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[3] : DESCRIPTIONS[0] + 25 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[3]);
    }

    @Override
    public float atDamageGive(float damage, DamageInfo.DamageType type) {
        if (type == DamageInfo.DamageType.NORMAL) {
            if (!this.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Crane")) {
                return damage * 0.6f;
            }
            return damage * 0.75f;
        }
        return damage;
    }
}

