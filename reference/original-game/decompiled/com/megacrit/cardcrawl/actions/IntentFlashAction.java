/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class IntentFlashAction
extends AbstractGameAction {
    private AbstractMonster m;

    public IntentFlashAction(AbstractMonster m) {
        this.startDuration = Settings.FAST_MODE ? Settings.ACTION_DUR_MED : Settings.ACTION_DUR_XLONG;
        this.duration = this.startDuration;
        this.m = m;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            this.m.flashIntent();
        }
        this.tickDuration();
    }
}

