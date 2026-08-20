/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class OddlySmoothStone
extends AbstractRelic {
    public static final String ID = "Oddly Smooth Stone";
    private static final int CON_AMT = 1;

    public OddlySmoothStone() {
        super(ID, "smooth_stone.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new DexterityPower(AbstractDungeon.player, 1), 1));
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new OddlySmoothStone();
    }
}

