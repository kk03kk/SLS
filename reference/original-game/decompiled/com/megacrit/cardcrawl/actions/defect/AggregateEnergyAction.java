/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class AggregateEnergyAction
extends AbstractGameAction {
    private int divideAmount;

    public AggregateEnergyAction(int divideAmountNum) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.divideAmount = divideAmountNum;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.player.gainEnergy(AbstractDungeon.player.drawPile.size() / this.divideAmount);
        }
        this.tickDuration();
    }
}

