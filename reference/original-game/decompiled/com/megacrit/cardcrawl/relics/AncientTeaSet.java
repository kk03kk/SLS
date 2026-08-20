/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class AncientTeaSet
extends AbstractRelic {
    public static final String ID = "Ancient Tea Set";
    private static final int ENERGY_AMT = 2;
    private boolean firstTurn = true;

    public AncientTeaSet() {
        super(ID, "tea_set.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void updateDescription(AbstractPlayer.PlayerClass c) {
        this.description = this.setDescription(c);
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void atTurnStart() {
        if (this.firstTurn) {
            if (this.counter == -2) {
                this.pulse = false;
                this.counter = -1;
                this.flash();
                this.addToTop(new GainEnergyAction(2));
                this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            }
            this.firstTurn = false;
        }
    }

    @Override
    public void atPreBattle() {
        this.firstTurn = true;
    }

    @Override
    public void setCounter(int counter) {
        super.setCounter(counter);
        if (counter == -2) {
            this.pulse = true;
        }
    }

    @Override
    public void onEnterRestRoom() {
        this.flash();
        this.counter = -2;
        this.pulse = true;
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 48;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new AncientTeaSet();
    }
}

