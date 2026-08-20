/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.EnemyTurnEffect;

public class EndTurnAction
extends AbstractGameAction {
    @Override
    public void update() {
        AbstractDungeon.actionManager.endTurn();
        if (!AbstractDungeon.getCurrRoom().skipMonsterTurn) {
            AbstractDungeon.topLevelEffects.add(new EnemyTurnEffect());
        }
        this.isDone = true;
    }
}

