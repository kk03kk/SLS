/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.WeakPower;

public class ForTheEyesAction
extends AbstractGameAction {
    private AbstractMonster m;

    public ForTheEyesAction(int weakAmt, AbstractMonster m) {
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.amount = weakAmt;
        this.m = m;
    }

    @Override
    public void update() {
        if (this.m != null && this.m.getIntentBaseDmg() >= 0) {
            this.addToTop(new ApplyPowerAction(this.m, AbstractDungeon.player, new WeakPower(this.m, this.amount, false), this.amount));
        }
        this.isDone = true;
    }
}

