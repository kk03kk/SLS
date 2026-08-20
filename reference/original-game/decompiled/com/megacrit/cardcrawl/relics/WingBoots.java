/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class WingBoots
extends AbstractRelic {
    public static final String ID = "WingedGreaves";

    public WingBoots() {
        super(ID, "winged.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
        this.counter = 3;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void setCounter(int setCounter) {
        this.counter = setCounter;
        if (this.counter == -2) {
            this.usedUp();
            this.counter = -2;
        }
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 40;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new WingBoots();
    }
}

