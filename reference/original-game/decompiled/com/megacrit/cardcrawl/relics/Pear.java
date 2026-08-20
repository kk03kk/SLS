/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Pear
extends AbstractRelic {
    public static final String ID = "Pear";
    private static final int HP_AMT = 10;

    public Pear() {
        super(ID, "pear.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 10 + LocalizedStrings.PERIOD;
    }

    @Override
    public void onEquip() {
        AbstractDungeon.player.increaseMaxHp(10, true);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Pear();
    }
}

