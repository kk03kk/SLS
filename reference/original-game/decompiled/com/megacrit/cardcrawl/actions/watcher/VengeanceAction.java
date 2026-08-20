/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class VengeanceAction
extends AbstractGameAction {
    @Override
    public void update() {
        if (GameActionManager.playerHpLastTurn > AbstractDungeon.player.currentHealth) {
            this.addToBot(new ChangeStanceAction("Wrath"));
        }
        this.isDone = true;
    }
}

