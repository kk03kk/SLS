/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

public class HealAction
extends AbstractGameAction {
    public HealAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.setValues(target, source, amount);
        this.startDuration = this.duration;
        if (Settings.FAST_MODE) {
            this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
        }
        this.actionType = AbstractGameAction.ActionType.HEAL;
    }

    public HealAction(AbstractCreature target, AbstractCreature source, int amount, float duration) {
        this(target, source, amount);
        this.duration = this.startDuration = duration;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            this.target.heal(this.amount);
        }
        this.tickDuration();
    }
}

