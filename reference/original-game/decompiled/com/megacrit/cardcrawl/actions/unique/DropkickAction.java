/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class DropkickAction
extends AbstractGameAction {
    private DamageInfo info;

    public DropkickAction(AbstractCreature target, DamageInfo info) {
        this.actionType = AbstractGameAction.ActionType.BLOCK;
        this.target = target;
        this.info = info;
    }

    @Override
    public void update() {
        if (this.target != null && this.target.hasPower("Vulnerable")) {
            this.addToTop(new DrawCardAction(AbstractDungeon.player, 1));
            this.addToTop(new GainEnergyAction(1));
        }
        this.addToTop(new DamageAction(this.target, this.info, AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        this.isDone = true;
    }
}

