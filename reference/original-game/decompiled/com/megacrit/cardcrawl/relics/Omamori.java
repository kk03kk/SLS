/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Omamori
extends AbstractRelic {
    public static final String ID = "Omamori";

    public Omamori() {
        super(ID, "omamori.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.FLAT);
        this.counter = 2;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void setCounter(int setCounter) {
        this.counter = setCounter;
        if (setCounter == 0) {
            this.usedUp();
        } else if (setCounter == 1) {
            this.description = this.DESCRIPTIONS[1];
        }
    }

    public void use() {
        this.flash();
        --this.counter;
        if (this.counter == 0) {
            this.setCounter(0);
        } else {
            this.description = this.DESCRIPTIONS[1];
        }
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 48;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Omamori();
    }
}

