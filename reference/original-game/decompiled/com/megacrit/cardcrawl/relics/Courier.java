/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;

public class Courier
extends AbstractRelic {
    public static final String ID = "The Courier";
    public static final float MULTIPLIER = 0.8f;

    public Courier() {
        super(ID, "courier.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEnterRoom(AbstractRoom room) {
        if (room instanceof ShopRoom) {
            this.flash();
            this.pulse = true;
        } else {
            this.pulse = false;
        }
    }

    @Override
    public boolean canSpawn() {
        return (Settings.isEndless || AbstractDungeon.floorNum <= 48) && !(AbstractDungeon.getCurrRoom() instanceof ShopRoom);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Courier();
    }
}

