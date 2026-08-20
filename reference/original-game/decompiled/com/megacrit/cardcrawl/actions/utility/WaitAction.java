/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;

public class WaitAction
extends AbstractGameAction {
    public WaitAction(float setDur) {
        this.setValues(null, null, 0);
        this.duration = Settings.FAST_MODE && setDur > 0.1f ? 0.1f : setDur;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        this.tickDuration();
    }
}

