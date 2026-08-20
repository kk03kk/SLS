/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.animations;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;

public class FastShakeAction
extends AbstractGameAction {
    private boolean called = false;
    private float shakeDur;

    public FastShakeAction(AbstractCreature owner, float shakeDur, float actionDur) {
        this.setValues(null, owner, 0);
        this.duration = actionDur;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.shakeDur = shakeDur;
    }

    @Override
    public void update() {
        if (!this.called) {
            this.source.useShakeAnimation(this.shakeDur);
            this.called = true;
        }
        this.tickDuration();
    }
}

