/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class MonsterStartTurnAction
extends AbstractGameAction {
    private static final float DURATION = Settings.ACTION_DUR_FAST;

    public MonsterStartTurnAction() {
        this.duration = DURATION;
    }

    @Override
    public void update() {
        if (this.duration == DURATION) {
            this.isDone = true;
            AbstractDungeon.getCurrRoom().monsters.applyPreTurnLogic();
        }
        this.tickDuration();
    }
}

