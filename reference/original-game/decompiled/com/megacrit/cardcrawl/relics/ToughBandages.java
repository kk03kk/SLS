/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class ToughBandages
extends AbstractRelic {
    public static final String ID = "Tough Bandages";
    private static final int BLOCK_AMT = 3;

    public ToughBandages() {
        super(ID, "tough_bandages.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onManualDiscard() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new GainBlockAction(AbstractDungeon.player, AbstractDungeon.player, 3, true));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new ToughBandages();
    }
}

