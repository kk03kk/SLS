/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;

public class BlackStar
extends AbstractRelic {
    public static final String ID = "Black Star";

    public BlackStar() {
        super(ID, "blackstar.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEnterRoom(AbstractRoom room) {
        if (room instanceof MonsterRoomElite) {
            this.pulse = true;
            this.beginPulse();
        } else {
            this.pulse = false;
        }
    }

    @Override
    public void onVictory() {
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomElite) {
            this.flash();
            this.pulse = false;
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BlackStar();
    }
}

