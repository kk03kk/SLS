/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class HandDrill
extends AbstractRelic {
    public static final String ID = "HandDrill";
    public static final int VULNERABLE_AMT = 2;

    public HandDrill() {
        super(ID, "drill.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onBlockBroken(AbstractCreature m) {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(m, this));
        this.addToBot(new ApplyPowerAction(m, AbstractDungeon.player, new VulnerablePower(m, 2, false), 2));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new HandDrill();
    }
}

