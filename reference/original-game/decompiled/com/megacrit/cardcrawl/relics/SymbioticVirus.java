/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.Dark;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class SymbioticVirus
extends AbstractRelic {
    public static final String ID = "Symbiotic Virus";

    public SymbioticVirus() {
        super(ID, "virus.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atPreBattle() {
        AbstractDungeon.player.channelOrb(new Dark());
    }

    @Override
    public AbstractRelic makeCopy() {
        return new SymbioticVirus();
    }
}

