/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;

public class LoseBlockAction
extends AbstractGameAction {
    public LoseBlockAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.setValues(target, source, amount);
        this.actionType = AbstractGameAction.ActionType.BLOCK;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            if (this.target.currentBlock == 0) {
                this.isDone = true;
                return;
            }
            this.target.loseBlock(this.amount);
        }
        this.tickDuration();
    }
}

