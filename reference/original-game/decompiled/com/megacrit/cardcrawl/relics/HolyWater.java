/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Miracle;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class HolyWater
extends AbstractRelic {
    public static final String ID = "HolyWater";

    public HolyWater() {
        super(ID, "holy_water.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStartPreDraw() {
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new MakeTempCardInHandAction((AbstractCard)new Miracle(), 3, false));
    }

    @Override
    public boolean canSpawn() {
        return AbstractDungeon.player.hasRelic("PureWater");
    }

    @Override
    public AbstractRelic makeCopy() {
        return new HolyWater();
    }
}

