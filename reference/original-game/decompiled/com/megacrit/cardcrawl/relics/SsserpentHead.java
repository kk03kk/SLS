/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;

public class SsserpentHead
extends AbstractRelic {
    public static final String ID = "SsserpentHead";
    private static final int GOLD_AMT = 50;

    public SsserpentHead() {
        super(ID, "serpentHead.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 50 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEnterRoom(AbstractRoom room) {
        if (room instanceof EventRoom) {
            this.flash();
            AbstractDungeon.player.gainGold(50);
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new SsserpentHead();
    }
}

