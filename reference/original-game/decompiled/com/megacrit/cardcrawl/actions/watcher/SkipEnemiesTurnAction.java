/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class SkipEnemiesTurnAction
extends AbstractGameAction {
    @Override
    public void update() {
        AbstractDungeon.getCurrRoom().skipMonsterTurn = true;
        this.isDone = true;
    }
}

