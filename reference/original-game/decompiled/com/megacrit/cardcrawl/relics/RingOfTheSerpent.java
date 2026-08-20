/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class RingOfTheSerpent
extends AbstractRelic {
    public static final String ID = "Ring of the Serpent";
    private static final int NUM_CARDS = 1;

    public RingOfTheSerpent() {
        super(ID, "serpent_ring.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEquip() {
        ++AbstractDungeon.player.masterHandSize;
    }

    @Override
    public void onUnequip() {
        --AbstractDungeon.player.masterHandSize;
    }

    @Override
    public void atTurnStart() {
        this.flash();
    }

    @Override
    public boolean canSpawn() {
        return AbstractDungeon.player.hasRelic("Ring of the Snake");
    }

    @Override
    public AbstractRelic makeCopy() {
        return new RingOfTheSerpent();
    }
}

