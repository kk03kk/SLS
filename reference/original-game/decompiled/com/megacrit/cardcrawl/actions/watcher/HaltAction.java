/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class HaltAction
extends AbstractGameAction {
    int additionalAmt;

    public HaltAction(AbstractCreature target, int block, int additional) {
        this.target = target;
        this.amount = block;
        this.additionalAmt = additional;
    }

    @Override
    public void update() {
        if (AbstractDungeon.player.stance.ID.equals("Wrath")) {
            this.addToTop(new GainBlockAction(this.target, this.amount + this.additionalAmt));
        } else {
            this.addToTop(new GainBlockAction(this.target, this.amount));
        }
        this.isDone = true;
    }
}

