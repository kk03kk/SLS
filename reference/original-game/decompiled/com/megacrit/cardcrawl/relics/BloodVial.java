/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BloodVial
extends AbstractRelic {
    public static final String ID = "Blood Vial";
    private static final int HEAL_AMOUNT = 2;

    public BloodVial() {
        super(ID, "blood_vial.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToTop(new HealAction(AbstractDungeon.player, AbstractDungeon.player, 2, 0.0f));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BloodVial();
    }
}

