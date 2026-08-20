/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;

public class MawBank
extends AbstractRelic {
    public static final String ID = "MawBank";
    private static final int GOLD_AMT = 12;

    public MawBank() {
        super(ID, "bank.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 12 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEnterRoom(AbstractRoom room) {
        if (!this.usedUp) {
            this.flash();
            AbstractDungeon.player.gainGold(12);
        }
    }

    @Override
    public void onSpendGold() {
        if (!this.usedUp) {
            this.flash();
            this.setCounter(-2);
        }
    }

    @Override
    public void setCounter(int setCounter) {
        this.counter = setCounter;
        if (setCounter == -2) {
            this.usedUp();
            this.counter = -2;
        }
    }

    @Override
    public boolean canSpawn() {
        return (Settings.isEndless || AbstractDungeon.floorNum <= 48) && !(AbstractDungeon.getCurrRoom() instanceof ShopRoom);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new MawBank();
    }
}

