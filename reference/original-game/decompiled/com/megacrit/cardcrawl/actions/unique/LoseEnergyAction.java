/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class LoseEnergyAction
extends AbstractGameAction {
    private int energyLoss;

    public LoseEnergyAction(int amount) {
        this.setValues(AbstractDungeon.player, AbstractDungeon.player, 0);
        this.energyLoss = amount;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.player.loseEnergy(this.energyLoss);
        }
        this.tickDuration();
    }
}

