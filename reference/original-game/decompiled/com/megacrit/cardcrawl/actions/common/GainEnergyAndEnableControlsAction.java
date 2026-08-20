/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class GainEnergyAndEnableControlsAction
extends AbstractGameAction {
    private int energyGain;

    public GainEnergyAndEnableControlsAction(int amount) {
        this.setValues(AbstractDungeon.player, AbstractDungeon.player, 0);
        this.energyGain = amount;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            AbstractDungeon.player.gainEnergy(this.energyGain);
            AbstractDungeon.actionManager.updateEnergyGain(this.energyGain);
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                c.triggerOnGainEnergy(this.energyGain, false);
            }
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onEnergyRecharge();
            }
            for (AbstractPower p : AbstractDungeon.player.powers) {
                p.onEnergyRecharge();
            }
            AbstractDungeon.actionManager.turnHasEnded = false;
        }
        this.tickDuration();
    }
}

