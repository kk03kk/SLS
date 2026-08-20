/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.defect.IncreaseMaxOrbAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class RunicCapacitor
extends AbstractRelic {
    public static final String ID = "Runic Capacitor";
    private boolean firstTurn = true;

    public RunicCapacitor() {
        super(ID, "runicCapacitor.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atPreBattle() {
        this.firstTurn = true;
    }

    @Override
    public void atTurnStart() {
        if (this.firstTurn) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new IncreaseMaxOrbAction(3));
            this.firstTurn = false;
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new RunicCapacitor();
    }
}

