/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class IncenseBurner
extends AbstractRelic {
    public static final String ID = "Incense Burner";
    private static final int NUM_TURNS = 6;

    public IncenseBurner() {
        super(ID, "incenseBurner.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        this.counter = 0;
    }

    @Override
    public void atTurnStart() {
        this.counter = this.counter == -1 ? (this.counter += 2) : ++this.counter;
        if (this.counter == 6) {
            this.counter = 0;
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new ApplyPowerAction(AbstractDungeon.player, null, new IntangiblePlayerPower(AbstractDungeon.player, 1), 1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new IncenseBurner();
    }
}

