/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Sundial
extends AbstractRelic {
    public static final String ID = "Sundial";
    private static final int NUM_TURNS = 3;
    private static final int ENERGY_AMT = 2;

    public Sundial() {
        super(ID, "sundial.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEquip() {
        this.counter = 0;
    }

    @Override
    public void onShuffle() {
        ++this.counter;
        if (this.counter == 3) {
            this.counter = 0;
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new GainEnergyAction(2));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Sundial();
    }
}

