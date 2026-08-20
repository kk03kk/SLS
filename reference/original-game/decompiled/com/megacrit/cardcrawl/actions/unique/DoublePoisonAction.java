/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.PoisonPower;

public class DoublePoisonAction
extends AbstractGameAction {
    public DoublePoisonAction(AbstractCreature target, AbstractCreature source) {
        this.target = target;
        this.source = source;
        this.actionType = AbstractGameAction.ActionType.DEBUFF;
        this.attackEffect = AbstractGameAction.AttackEffect.FIRE;
    }

    @Override
    public void update() {
        if (this.target != null && this.target.hasPower("Poison")) {
            this.addToTop(new ApplyPowerAction(this.target, this.source, new PoisonPower(this.target, this.source, this.target.getPower((String)"Poison").amount), this.target.getPower((String)"Poison").amount));
        }
        this.isDone = true;
    }
}

