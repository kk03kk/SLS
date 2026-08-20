/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BustedCrown
extends AbstractRelic {
    private static final int CARDS_SUBTRACTED = 2;
    public static final String ID = "Busted Crown";

    public BustedCrown() {
        super(ID, "crown.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[1] + this.DESCRIPTIONS[0];
    }

    @Override
    public void updateDescription(AbstractPlayer.PlayerClass c) {
        this.description = this.setDescription(c);
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
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
    public int changeNumberOfCardsInReward(int numberOfCards) {
        return numberOfCards - 2;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BustedCrown();
    }
}

