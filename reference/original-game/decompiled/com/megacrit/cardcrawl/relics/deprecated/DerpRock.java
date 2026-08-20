/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics.deprecated;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class DerpRock
extends AbstractRelic {
    public static final String ID = "Derp Rock";
    public static final int CHARGE_AMT = 1;

    public DerpRock() {
        super(ID, "derpRock.png", AbstractRelic.RelicTier.STARTER, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atPreBattle() {
        AbstractDungeon.actionManager.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new DerpRock();
    }
}

