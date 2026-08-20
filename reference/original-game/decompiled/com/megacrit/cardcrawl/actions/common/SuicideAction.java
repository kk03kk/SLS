/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class SuicideAction
extends AbstractGameAction {
    private AbstractMonster m;
    private boolean relicTrigger;

    public SuicideAction(AbstractMonster target) {
        this(target, true);
    }

    public SuicideAction(AbstractMonster target, boolean triggerRelics) {
        this.duration = 0.0f;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.m = target;
        this.relicTrigger = triggerRelics;
    }

    @Override
    public void update() {
        if (this.duration == 0.0f) {
            this.m.gold = 0;
            this.m.currentHealth = 0;
            this.m.die(this.relicTrigger);
            this.m.healthBarUpdatedEvent();
        }
        this.tickDuration();
    }
}

