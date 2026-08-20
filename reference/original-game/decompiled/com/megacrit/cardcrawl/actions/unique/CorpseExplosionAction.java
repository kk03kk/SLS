/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

public class CorpseExplosionAction
extends AbstractGameAction {
    public CorpseExplosionAction(AbstractCreature target, AbstractCreature source) {
        this.target = target;
        this.source = source;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.target.hasPower("Poison")) {
                this.addToTop(new RemoveSpecificPowerAction(this.target, this.source, "Poison"));
            } else {
                this.isDone = true;
            }
        }
        this.tickDuration();
    }
}

