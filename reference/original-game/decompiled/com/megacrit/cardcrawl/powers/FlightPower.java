/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class FlightPower
extends AbstractPower {
    public static final String POWER_ID = "Flight";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Flight");
    public static final String NAME = FlightPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = FlightPower.powerStrings.DESCRIPTIONS;
    private int storedAmount;

    public FlightPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.storedAmount = amount;
        this.updateDescription();
        this.loadRegion("flight");
        this.priority = 50;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_FLIGHT", 0.05f);
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void atStartOfTurn() {
        this.amount = this.storedAmount;
        this.updateDescription();
    }

    @Override
    public float atDamageFinalReceive(float damage, DamageInfo.DamageType type) {
        return this.calculateDamageTakenAmount(damage, type);
    }

    private float calculateDamageTakenAmount(float damage, DamageInfo.DamageType type) {
        if (type != DamageInfo.DamageType.HP_LOSS && type != DamageInfo.DamageType.THORNS) {
            return damage / 2.0f;
        }
        return damage;
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        Boolean willLive = this.calculateDamageTakenAmount(damageAmount, info.type) < (float)this.owner.currentHealth;
        if (info.owner != null && info.type != DamageInfo.DamageType.HP_LOSS && info.type != DamageInfo.DamageType.THORNS && damageAmount > 0 && willLive.booleanValue()) {
            this.flash();
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
        }
        return damageAmount;
    }

    @Override
    public void onRemove() {
        this.addToBot(new ChangeStateAction((AbstractMonster)this.owner, "GROUNDED"));
    }
}

