/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class Anchor
extends AbstractRelic {
    public static final String ID = "Anchor";
    private static final int BLOCK_AMT = 10;

    public Anchor() {
        super(ID, "anchor.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 10 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, 10));
        this.grayscale = true;
    }

    @Override
    public void justEnteredRoom(AbstractRoom room) {
        this.grayscale = false;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Anchor();
    }
}

