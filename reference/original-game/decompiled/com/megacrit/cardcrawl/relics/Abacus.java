/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Abacus
extends AbstractRelic {
    public static final String ID = "TheAbacus";
    private static final int BLOCK_AMT = 6;

    public Abacus() {
        super(ID, "abacus.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 6 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onShuffle() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, 6));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Abacus();
    }
}

