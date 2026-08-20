/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Test6
extends AbstractRelic {
    public static final String ID = "Test 6";
    private static final int GOLD_REQ = 100;
    private static final int BLOCK_AMT = 3;

    public Test6() {
        super(ID, "test6.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1] + 100 + this.DESCRIPTIONS[2];
    }

    @Override
    public void onPlayerEndTurn() {
        if (this.hasEnoughGold()) {
            this.flash();
            this.pulse = false;
            this.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, 3 * (AbstractDungeon.player.gold / 100)));
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        }
    }

    @Override
    public void atTurnStart() {
        if (this.hasEnoughGold()) {
            this.pulse = true;
            this.beginPulse();
        }
    }

    @Override
    public void onVictory() {
        this.pulse = false;
    }

    private boolean hasEnoughGold() {
        return AbstractDungeon.player.gold >= 100;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Test6();
    }
}

