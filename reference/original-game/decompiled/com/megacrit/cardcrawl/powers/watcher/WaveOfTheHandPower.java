/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.WeakPower;

public class WaveOfTheHandPower
extends AbstractPower {
    public static final String POWER_ID = "WaveOfTheHandPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("WaveOfTheHandPower");

    public WaveOfTheHandPower(AbstractCreature owner, int newAmount) {
        this.name = WaveOfTheHandPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.updateDescription();
        this.loadRegion("wave_of_the_hand");
    }

    @Override
    public void onGainedBlock(float blockAmount) {
        if (blockAmount > 0.0f) {
            this.flash();
            AbstractPlayer p = AbstractDungeon.player;
            for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
                this.addToBot(new ApplyPowerAction(mo, p, new WeakPower(mo, this.amount, false), this.amount, true, AbstractGameAction.AttackEffect.NONE));
            }
        }
    }

    @Override
    public void atEndOfRound() {
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
    }

    @Override
    public void updateDescription() {
        this.description = WaveOfTheHandPower.powerStrings.DESCRIPTIONS[0] + this.amount + WaveOfTheHandPower.powerStrings.DESCRIPTIONS[1];
    }
}

