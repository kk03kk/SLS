/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class VelvetChoker
extends AbstractRelic {
    public static final String ID = "Velvet Choker";
    private static final int PLAY_LIMIT = 6;

    public VelvetChoker() {
        super(ID, "redChoker.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[2] + this.DESCRIPTIONS[0] + 6 + this.DESCRIPTIONS[1];
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
    public void atBattleStart() {
        this.counter = 0;
    }

    @Override
    public void atTurnStart() {
        this.counter = 0;
    }

    @Override
    public void onPlayCard(AbstractCard card, AbstractMonster m) {
        if (this.counter < 6) {
            ++this.counter;
            if (this.counter >= 6) {
                this.flash();
            }
        }
    }

    @Override
    public boolean canPlay(AbstractCard card) {
        if (this.counter >= 6) {
            card.cantUseMessage = this.DESCRIPTIONS[3] + 6 + this.DESCRIPTIONS[1];
            return false;
        }
        return true;
    }

    @Override
    public void onVictory() {
        this.counter = -1;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new VelvetChoker();
    }
}

