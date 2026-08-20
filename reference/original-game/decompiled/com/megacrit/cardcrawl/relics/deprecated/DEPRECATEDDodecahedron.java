/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class DEPRECATEDDodecahedron
extends AbstractRelic {
    public static final String ID = "Dodecahedron";
    private static final int ENERGY_AMT = 1;

    public DEPRECATEDDodecahedron() {
        super(ID, "dodecahedron.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.HEAVY);
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
    public void atBattleStart() {
        this.controlPulse();
    }

    @Override
    public void onVictory() {
        this.stopPulse();
    }

    @Override
    public void atTurnStart() {
        this.addToBot(new AbstractGameAction(){

            @Override
            public void update() {
                if (DEPRECATEDDodecahedron.this.isActive()) {
                    DEPRECATEDDodecahedron.this.flash();
                    this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, DEPRECATEDDodecahedron.this));
                    this.addToBot(new GainEnergyAction(1));
                }
                this.isDone = true;
            }
        });
    }

    @Override
    public int onPlayerHeal(int healAmount) {
        this.controlPulse();
        return super.onPlayerHeal(healAmount);
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (damageAmount > 0) {
            this.stopPulse();
        }
        return super.onAttacked(info, damageAmount);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new DEPRECATEDDodecahedron();
    }

    private boolean isActive() {
        return AbstractDungeon.player.currentHealth >= AbstractDungeon.player.maxHealth;
    }

    private void controlPulse() {
        if (this.isActive()) {
            this.beginLongPulse();
        } else {
            this.stopPulse();
        }
    }
}

