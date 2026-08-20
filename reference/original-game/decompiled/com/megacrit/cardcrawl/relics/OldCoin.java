/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.ShopRoom;

public class OldCoin
extends AbstractRelic {
    public static final String ID = "Old Coin";
    private static final int GOLD_AMT = 300;

    public OldCoin() {
        super(ID, "oldCoin.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        CardCrawlGame.sound.play("GOLD_GAIN");
        AbstractDungeon.player.gainGold(300);
    }

    @Override
    public boolean canSpawn() {
        return (Settings.isEndless || AbstractDungeon.floorNum <= 48) && !(AbstractDungeon.getCurrRoom() instanceof ShopRoom);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new OldCoin();
    }
}

