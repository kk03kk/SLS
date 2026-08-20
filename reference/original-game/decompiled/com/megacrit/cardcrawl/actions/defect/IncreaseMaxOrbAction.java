/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class IncreaseMaxOrbAction
extends AbstractGameAction {
    public IncreaseMaxOrbAction(int slotIncrease) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = slotIncrease;
        this.actionType = AbstractGameAction.ActionType.BLOCK;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            for (int i = 0; i < this.amount; ++i) {
                AbstractDungeon.player.increaseMaxOrbSlots(1, true);
            }
        }
        this.tickDuration();
    }
}

