/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Mango
extends AbstractRelic {
    public static final String ID = "Mango";
    private static final int HP_AMT = 14;

    public Mango() {
        super(ID, "mango.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 14 + LocalizedStrings.PERIOD;
    }

    @Override
    public void onEquip() {
        AbstractDungeon.player.increaseMaxHp(14, true);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Mango();
    }
}

