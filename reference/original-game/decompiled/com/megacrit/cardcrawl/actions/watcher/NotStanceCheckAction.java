/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class NotStanceCheckAction
extends AbstractGameAction {
    private AbstractGameAction actionToBuffer;
    private String stanceToCheck = null;

    public NotStanceCheckAction(String stanceToCheck, AbstractGameAction actionToCheck) {
        this.actionToBuffer = actionToCheck;
        this.stanceToCheck = stanceToCheck;
    }

    @Override
    public void update() {
        if (!AbstractDungeon.player.stance.ID.equals(this.stanceToCheck)) {
            this.addToBot(this.actionToBuffer);
        }
        this.isDone = true;
    }
}

