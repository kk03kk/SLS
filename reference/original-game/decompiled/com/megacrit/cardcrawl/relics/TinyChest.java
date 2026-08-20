/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class TinyChest
extends AbstractRelic {
    public static final String ID = "Tiny Chest";
    public static final int ROOM_COUNT = 4;

    public TinyChest() {
        super(ID, "tinyChest.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.SOLID);
        this.counter = -1;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 4 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEquip() {
        this.counter = 0;
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 35;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new TinyChest();
    }
}

