/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;

public class EternalFeather
extends AbstractRelic {
    public static final String ID = "Eternal Feather";

    public EternalFeather() {
        super(ID, "eternal_feather.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 5 + this.DESCRIPTIONS[1] + 3 + this.DESCRIPTIONS[2];
    }

    @Override
    public void onEnterRoom(AbstractRoom room) {
        if (room instanceof RestRoom) {
            this.flash();
            int amountToGain = AbstractDungeon.player.masterDeck.size() / 5 * 3;
            AbstractDungeon.player.heal(amountToGain);
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new EternalFeather();
    }
}

