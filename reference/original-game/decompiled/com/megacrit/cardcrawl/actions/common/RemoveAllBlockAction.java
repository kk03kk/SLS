/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;

public class RemoveAllBlockAction
extends AbstractGameAction {
    private static final float DUR = 0.25f;

    public RemoveAllBlockAction(AbstractCreature target, AbstractCreature source) {
        this.setValues(target, source, this.amount);
        this.actionType = AbstractGameAction.ActionType.BLOCK;
        this.duration = 0.25f;
    }

    @Override
    public void update() {
        if (!this.target.isDying && !this.target.isDead && this.duration == 0.25f && this.target.currentBlock > 0) {
            this.target.loseBlock();
        }
        this.tickDuration();
    }
}

