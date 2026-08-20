/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class EmptyBodyAction
extends AbstractGameAction {
    private int additionalDraw;

    public EmptyBodyAction(int additionalDraw) {
        this.additionalDraw = additionalDraw;
    }

    @Override
    public void update() {
        if (AbstractDungeon.player.stance.ID.equals("Neutral")) {
            this.addToBot(new ChangeStanceAction("Neutral"));
            this.addToBot(new DrawCardAction(1 + this.additionalDraw));
        } else {
            this.addToBot(new DrawCardAction(1));
        }
        this.isDone = true;
    }
}

