/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class NuclearBattery
extends AbstractRelic {
    public static final String ID = "Nuclear Battery";

    public NuclearBattery() {
        super(ID, "battery.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atPreBattle() {
        AbstractDungeon.player.channelOrb(new Plasma());
    }

    @Override
    public AbstractRelic makeCopy() {
        return new NuclearBattery();
    }
}

