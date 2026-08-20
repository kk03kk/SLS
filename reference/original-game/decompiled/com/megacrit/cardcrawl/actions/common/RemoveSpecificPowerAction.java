/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.PowerExpireTextEffect;

public class RemoveSpecificPowerAction
extends AbstractGameAction {
    private String powerToRemove;
    private AbstractPower powerInstance;
    private static final float DURATION = 0.1f;

    public RemoveSpecificPowerAction(AbstractCreature target, AbstractCreature source, String powerToRemove) {
        this.setValues(target, source, this.amount);
        this.actionType = AbstractGameAction.ActionType.DEBUFF;
        this.duration = 0.1f;
        this.powerToRemove = powerToRemove;
    }

    public RemoveSpecificPowerAction(AbstractCreature target, AbstractCreature source, AbstractPower powerInstance) {
        this.setValues(target, source, this.amount);
        this.actionType = AbstractGameAction.ActionType.DEBUFF;
        this.duration = 0.1f;
        this.powerInstance = powerInstance;
    }

    @Override
    public void update() {
        if (this.duration == 0.1f) {
            if (this.target.isDeadOrEscaped()) {
                this.isDone = true;
                return;
            }
            AbstractPower removeMe = null;
            if (this.powerToRemove != null) {
                removeMe = this.target.getPower(this.powerToRemove);
            } else if (this.powerInstance != null && this.target.powers.contains(this.powerInstance)) {
                removeMe = this.powerInstance;
            }
            if (removeMe != null) {
                AbstractDungeon.effectList.add(new PowerExpireTextEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, removeMe.name, removeMe.region128));
                removeMe.onRemove();
                this.target.powers.remove(removeMe);
                AbstractDungeon.onModifyPower();
                for (AbstractOrb o : AbstractDungeon.player.orbs) {
                    o.updateDescription();
                }
            } else {
                this.duration = 0.0f;
            }
        }
        this.tickDuration();
    }
}

