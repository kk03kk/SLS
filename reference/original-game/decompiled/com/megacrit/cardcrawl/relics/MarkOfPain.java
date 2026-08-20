/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class MarkOfPain
extends AbstractRelic {
    public static final String ID = "Mark of Pain";
    private static final int CARD_AMT = 2;

    public MarkOfPain() {
        super(ID, "mark_of_pain.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new MakeTempCardInDrawPileAction(new Wound(), 2, true, true));
    }

    @Override
    public void onEquip() {
        ++AbstractDungeon.player.energy.energyMaster;
    }

    @Override
    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new MarkOfPain();
    }
}

