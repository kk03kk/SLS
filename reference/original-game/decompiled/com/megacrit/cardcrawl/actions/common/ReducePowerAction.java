/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ReducePowerAction
extends AbstractGameAction {
    private String powerID;
    private AbstractPower powerInstance;

    public ReducePowerAction(AbstractCreature target, AbstractCreature source, String power, int amount) {
        this.setValues(target, source, amount);
        this.duration = Settings.FAST_MODE ? (this.startDuration = Settings.ACTION_DUR_XFAST) : (this.startDuration = Settings.ACTION_DUR_FAST);
        this.powerID = power;
        this.actionType = AbstractGameAction.ActionType.REDUCE_POWER;
    }

    public ReducePowerAction(AbstractCreature target, AbstractCreature source, AbstractPower powerInstance, int amount) {
        this.setValues(target, source, amount);
        this.duration = Settings.FAST_MODE ? (this.startDuration = Settings.ACTION_DUR_XFAST) : (this.startDuration = Settings.ACTION_DUR_FAST);
        this.powerInstance = powerInstance;
        this.actionType = AbstractGameAction.ActionType.REDUCE_POWER;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            AbstractPower reduceMe = null;
            if (this.powerID != null) {
                reduceMe = this.target.getPower(this.powerID);
            } else if (this.powerInstance != null) {
                reduceMe = this.powerInstance;
            }
            if (reduceMe != null) {
                if (this.amount < reduceMe.amount) {
                    reduceMe.reducePower(this.amount);
                    reduceMe.updateDescription();
                    AbstractDungeon.onModifyPower();
                } else {
                    this.addToTop(new RemoveSpecificPowerAction(this.target, this.source, reduceMe));
                }
            }
        }
        this.tickDuration();
    }
}

