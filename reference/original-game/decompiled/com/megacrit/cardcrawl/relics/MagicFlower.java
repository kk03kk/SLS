/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class MagicFlower
extends AbstractRelic {
    public static final String ID = "Magic Flower";
    private static final float HEAL_MULTIPLIER = 1.5f;

    public MagicFlower() {
        super(ID, "magicFlower.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public int onPlayerHeal(int healAmount) {
        if (AbstractDungeon.currMapNode != null && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.flash();
            return MathUtils.round((float)healAmount * 1.5f);
        }
        return healAmount;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new MagicFlower();
    }
}

