/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class CrackedCore
extends AbstractRelic {
    public static final String ID = "Cracked Core";

    public CrackedCore() {
        super(ID, "crackedOrb.png", AbstractRelic.RelicTier.STARTER, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atPreBattle() {
        AbstractDungeon.player.channelOrb(new Lightning());
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CrackedCore();
    }
}

