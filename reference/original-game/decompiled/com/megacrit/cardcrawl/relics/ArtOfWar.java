/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class ArtOfWar
extends AbstractRelic {
    public static final String ID = "Art of War";
    private boolean gainEnergyNext = false;
    private boolean firstTurn = false;

    public ArtOfWar() {
        super(ID, "artOfWar.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.FLAT);
        this.pulse = false;
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[0] + this.DESCRIPTIONS[1];
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
        this.flash();
        this.firstTurn = true;
        this.gainEnergyNext = true;
        if (!this.pulse) {
            this.beginPulse();
            this.pulse = true;
        }
    }

    @Override
    public void atTurnStart() {
        this.beginPulse();
        this.pulse = true;
        if (this.gainEnergyNext && !this.firstTurn) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new GainEnergyAction(1));
        }
        this.firstTurn = false;
        this.gainEnergyNext = true;
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.ATTACK) {
            this.gainEnergyNext = false;
            this.pulse = false;
        }
    }

    @Override
    public void onVictory() {
        this.pulse = false;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new ArtOfWar();
    }
}

