/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class InnerPeaceAction
extends AbstractGameAction {
    public InnerPeaceAction(int amount) {
        this.amount = amount;
    }

    @Override
    public void update() {
        if (AbstractDungeon.player.stance.ID.equals("Calm")) {
            this.addToTop(new DrawCardAction(this.amount));
        } else {
            this.addToTop(new ChangeStanceAction("Calm"));
        }
        this.isDone = true;
    }
}

