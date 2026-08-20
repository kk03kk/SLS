/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class GainEnergyAction
extends AbstractGameAction {
    private int energyGain;

    public GainEnergyAction(int amount) {
        this.setValues(AbstractDungeon.player, AbstractDungeon.player, 0);
        this.duration = Settings.ACTION_DUR_FAST;
        this.energyGain = amount;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.player.gainEnergy(this.energyGain);
            AbstractDungeon.actionManager.updateEnergyGain(this.energyGain);
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                c.triggerOnGainEnergy(this.energyGain, true);
            }
        }
        this.tickDuration();
    }
}

