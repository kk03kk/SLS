/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class SnakeRing
extends AbstractRelic {
    public static final String ID = "Ring of the Snake";
    private static final int NUM_CARDS = 2;

    public SnakeRing() {
        super(ID, "snake_ring.png", AbstractRelic.RelicTier.STARTER, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new DrawCardAction(AbstractDungeon.player, 2));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new SnakeRing();
    }
}

