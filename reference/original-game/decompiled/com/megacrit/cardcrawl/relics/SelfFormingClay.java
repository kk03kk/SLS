/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.NextTurnBlockPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class SelfFormingClay
extends AbstractRelic {
    public static final String ID = "Self Forming Clay";
    private static final int BLOCK_AMT = 3;

    public SelfFormingClay() {
        super(ID, "clay.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public void wasHPLost(int damageAmount) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && damageAmount > 0) {
            this.flash();
            this.addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new NextTurnBlockPower(AbstractDungeon.player, 3, this.name), 3));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new SelfFormingClay();
    }
}

