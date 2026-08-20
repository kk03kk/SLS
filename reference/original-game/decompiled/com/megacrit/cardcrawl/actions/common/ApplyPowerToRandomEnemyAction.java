/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ApplyPowerToRandomEnemyAction
extends AbstractGameAction {
    private AbstractPower powerToApply;
    private boolean isFast;
    private AbstractGameAction.AttackEffect effect;

    public ApplyPowerToRandomEnemyAction(AbstractCreature source, AbstractPower powerToApply, int stackAmount, boolean isFast, AbstractGameAction.AttackEffect effect) {
        this.setValues(null, source, stackAmount);
        this.powerToApply = powerToApply;
        this.isFast = isFast;
        this.effect = effect;
    }

    public ApplyPowerToRandomEnemyAction(AbstractCreature source, AbstractPower powerToApply, int stackAmount, boolean isFast) {
        this(source, powerToApply, stackAmount, isFast, AbstractGameAction.AttackEffect.NONE);
    }

    public ApplyPowerToRandomEnemyAction(AbstractCreature source, AbstractPower powerToApply, int stackAmount) {
        this(source, powerToApply, stackAmount, false);
    }

    public ApplyPowerToRandomEnemyAction(AbstractCreature source, AbstractPower powerToApply) {
        this(source, powerToApply, -1);
    }

    @Override
    public void update() {
        this.powerToApply.owner = this.target = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);
        if (this.target != null) {
            this.addToTop(new ApplyPowerAction(this.target, this.source, this.powerToApply, this.amount, this.isFast, this.effect));
        }
        this.isDone = true;
    }
}

