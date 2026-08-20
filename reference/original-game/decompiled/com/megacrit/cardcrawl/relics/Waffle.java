/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Waffle
extends AbstractRelic {
    public static final String ID = "Lee's Waffle";
    private static final int HP_AMT = 7;

    public Waffle() {
        super(ID, "waffle.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 7 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEquip() {
        AbstractDungeon.player.increaseMaxHp(7, false);
        AbstractDungeon.player.heal(AbstractDungeon.player.maxHealth);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Waffle();
    }
}

