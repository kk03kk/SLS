/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class GainGoldAction
extends AbstractGameAction {
    public GainGoldAction(int amount) {
        this.amount = amount;
    }

    @Override
    public void update() {
        AbstractDungeon.player.gainGold(this.amount);
        this.isDone = true;
    }
}

