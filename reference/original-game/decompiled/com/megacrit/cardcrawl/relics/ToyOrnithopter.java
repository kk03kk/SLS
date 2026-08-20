/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class ToyOrnithopter
extends AbstractRelic {
    public static final String ID = "Toy Ornithopter";
    public static final int HEAL_AMT = 5;

    public ToyOrnithopter() {
        super(ID, "ornithopter.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 5 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onUsePotion() {
        this.flash();
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new HealAction(AbstractDungeon.player, AbstractDungeon.player, 5));
        } else {
            AbstractDungeon.player.heal(5);
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new ToyOrnithopter();
    }
}

