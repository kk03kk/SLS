/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.unique.GamblingChipAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class GamblingChip
extends AbstractRelic {
    public static final String ID = "Gambling Chip";
    private boolean activated = false;

    public GamblingChip() {
        super(ID, "gamblingChip.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStartPreDraw() {
        this.activated = false;
    }

    @Override
    public void atTurnStartPostDraw() {
        if (!this.activated) {
            this.activated = true;
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new GamblingChipAction(AbstractDungeon.player));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new GamblingChip();
    }
}

