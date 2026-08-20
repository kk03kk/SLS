/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class CentennialPuzzle
extends AbstractRelic {
    public static final String ID = "Centennial Puzzle";
    private static final int NUM_CARDS = 3;
    private static boolean usedThisCombat = false;

    public CentennialPuzzle() {
        super(ID, "centennialPuzzle.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atPreBattle() {
        usedThisCombat = false;
        this.pulse = true;
        this.beginPulse();
    }

    @Override
    public void wasHPLost(int damageAmount) {
        if (damageAmount > 0 && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !usedThisCombat) {
            this.flash();
            this.pulse = false;
            this.addToTop(new DrawCardAction(AbstractDungeon.player, 3));
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            usedThisCombat = true;
            this.grayscale = true;
        }
    }

    @Override
    public void justEnteredRoom(AbstractRoom room) {
        this.grayscale = false;
    }

    @Override
    public void onVictory() {
        this.pulse = false;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CentennialPuzzle();
    }
}

