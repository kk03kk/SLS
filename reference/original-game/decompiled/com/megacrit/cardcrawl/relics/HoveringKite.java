/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class HoveringKite
extends AbstractRelic {
    public static final String ID = "HoveringKite";
    private boolean triggeredThisTurn = false;

    public HoveringKite() {
        super(ID, "kite.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atTurnStart() {
        this.triggeredThisTurn = false;
    }

    @Override
    public void onManualDiscard() {
        if (!this.triggeredThisTurn) {
            this.triggeredThisTurn = true;
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new GainEnergyAction(1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new HoveringKite();
    }
}

