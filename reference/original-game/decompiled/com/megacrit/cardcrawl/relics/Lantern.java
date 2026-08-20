/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Lantern
extends AbstractRelic {
    public static final String ID = "Lantern";
    private static final int ENERGY_AMT = 1;
    private boolean firstTurn = true;

    public Lantern() {
        super(ID, "lantern.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.SOLID);
        this.energyBased = true;
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[0] + this.DESCRIPTIONS[1] + LocalizedStrings.PERIOD;
    }

    @Override
    public void updateDescription(AbstractPlayer.PlayerClass c) {
        this.description = this.setDescription(c);
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void atPreBattle() {
        this.firstTurn = true;
    }

    @Override
    public void atTurnStart() {
        if (this.firstTurn) {
            this.flash();
            this.addToTop(new GainEnergyAction(1));
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.firstTurn = false;
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Lantern();
    }
}

